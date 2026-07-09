//! Port of (the read side of) `UserStorageBo` — the player record and the

use crate::bo::{FactionBo, PlanetBo, UserImprovementBo};
use crate::dto::{SimpleUserData, UserData};
use crate::error::{OwgeError, OwgeResult};
use crate::model::UserStorage;
use sqlx::{Connection, MySqlConnection};

pub struct UserStorageBo;

/// The faction columns `subscribe` seeds the new user's resources from.
#[derive(sqlx::FromRow)]
struct FactionInitRow {
    #[allow(dead_code)]
    id: u16,
    /// `initial_primary_resource` is `mediumint unsigned`.
    initial_primary_resource: u32,
    initial_secondary_resource: u32,
    initial_energy: u32,
}

impl UserStorageBo {
    pub async fn find_by_id(
        conn: &mut MySqlConnection,
        id: i32,
    ) -> OwgeResult<Option<UserStorage>> {
        Ok(UserStorage::find_one_by_id(&id, &mut *conn).await?)
    }

    /// `UserStorageBo.subscribe(factionId)` — provision the logged-in account
    /// into this universe: validate the faction, pick a random free spawn planet
    /// in one of the faction's spawn galaxies (universe-wide if the faction has
    /// none), seed starting resources, mark the planet owned + home, and fire the
    /// faction/home-galaxy requirement triggers so the player's starting content
    /// is unlocked. Returns `false` if already subscribed (matching Java).
    ///
    /// `token` is the lightweight user from the JWT (`findLoggedIn`): it carries
    /// the account `id`/`username`/`email` that become the new `user_storage` row
    /// (the id is the external account id, not auto-increment).
    ///
    /// The whole operation runs in one transaction so a failed trigger can't
    /// leave a half-provisioned account. The `audit` call (dropped, not ported)
    /// and websocket emissions (M4) are intentionally omitted.
    pub async fn subscribe(
        conn: &mut MySqlConnection,
        token: &crate::jwt::TokenUser,
        faction_id: u16,
    ) -> OwgeResult<bool> {
        // Faction must exist (SgtFactionNotFoundException -> 404).
        let faction = sqlx::query_as::<_, FactionInitRow>(
            "SELECT id, initial_primary_resource, initial_secondary_resource, initial_energy \
             FROM factions WHERE id = ?",
        )
        .bind(faction_id)
        .fetch_optional(&mut *conn)
        .await?
        .ok_or_else(|| OwgeError::NotFound("No such faction".into()))?;

        let user_id = token.id as i32;
        if Self::exists(&mut *conn, user_id).await? {
            return Ok(false);
        }

        let mut tx = conn.begin().await?;

        // determineSpawnGalaxy(faction): random spawn galaxy for the faction, or
        // None -> universe-wide planet pick.
        let spawn_galaxy = sqlx::query_scalar::<_, u16>(
            "SELECT galaxy_id FROM faction_spawn_location WHERE faction_id = ? ORDER BY RAND() LIMIT 1",
        )
        .bind(faction_id)
        .fetch_optional(&mut *tx)
        .await?;

        // findRandomPlanet: a free planet (no owner, no special location) in the
        // chosen galaxy, or universe-wide. SgtBackendUniverseIsFull -> no space.
        let planet_id: Option<u64> = if let Some(galaxy_id) = spawn_galaxy {
            sqlx::query_scalar(
                "SELECT id FROM planets \
                 WHERE galaxy_id = ? AND owner IS NULL AND special_location_id IS NULL \
                 ORDER BY RAND() LIMIT 1",
            )
            .bind(galaxy_id)
            .fetch_optional(&mut *tx)
            .await?
        } else {
            sqlx::query_scalar(
                "SELECT id FROM planets \
                 WHERE owner IS NULL AND special_location_id IS NULL \
                 ORDER BY RAND() LIMIT 1",
            )
            .fetch_optional(&mut *tx)
            .await?
        };
        let planet_id = planet_id
            .ok_or_else(|| OwgeError::InvalidInput("No hay más espacio en este universo".into()))?;

        // Insert the user_storage row (id = account id; not auto-increment).
        // Boolean/points defaults mirror the JPA entity field initializers.
        sqlx::query(
            "INSERT INTO user_storage \
                (id, username, email, faction, last_action, home_planet, \
                 primary_resource, secondary_resource, energy, \
                 has_skipped_tutorial, points, can_alter_twitch_state, banned) \
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 0)",
        )
        .bind(user_id)
        .bind(&token.username)
        .bind(&token.email)
        .bind(faction_id)
        .bind(chrono::Utc::now().naive_utc())
        .bind(planet_id)
        .bind(faction.initial_primary_resource as f64)
        .bind(faction.initial_secondary_resource as f64)
        .bind(faction.initial_energy as f64)
        .execute(&mut *tx)
        .await?;

        // Mark the spawn planet as owned + home.
        sqlx::query("UPDATE planets SET owner = ?, home = 1 WHERE id = ?")
            .bind(user_id)
            .bind(planet_id)
            .execute(&mut *tx)
            .await?;

        // Load the freshly-persisted user so the triggers see its faction/home
        // planet, then fire them (they write `unlocked_relation`/`obtained_upgrades`).
        let user = Self::find_by_id_conn(&mut tx, user_id)
            .await?
            .ok_or_else(|| {
                OwgeError::Common("User vanished right after subscribe insert".into())
            })?;
        let mut req_emits = Vec::new();
        crate::bo::requirement_engine::trigger_faction_selection(&mut tx, &user, &mut req_emits)
            .await?;
        crate::bo::requirement_engine::trigger_home_galaxy_selection(
            &mut tx,
            &user,
            &mut req_emits,
        )
        .await?;

        tx.commit().await?;
        // Post-commit: the unit/time-special/speed-impact-group unlocks the
        // faction/home-galaxy selection produced (Java doAfterCommit emits).
        crate::bo::realtime_emitter::drain_requirement_emits(&mut *conn, &req_emits).await?;
        // NOT PORTED (deliberate): auditBo.doAudit(SUBSCRIBE_TO_WORLD) — auditing is
        // disabled in the live Java deployment, so it is an intentional no-op.
        Ok(user_id > 0)
    }

    /// Same as [`find_by_id`](Self::find_by_id) but on an existing connection /
    /// transaction (so `subscribe` can read its own just-inserted row).
    async fn find_by_id_conn(
        conn: &mut sqlx::MySqlConnection,
        id: i32,
    ) -> OwgeResult<Option<UserStorage>> {
        let row = sqlx::query_as::<_, UserStorage>(
            "SELECT id, username, email, alliance_id, faction, last_action, home_planet, \
                    primary_resource, secondary_resource, energy, \
                    primary_resource_generation_per_second, secondary_resource_generation_per_second, \
                    has_skipped_tutorial, points, can_alter_twitch_state, banned \
             FROM user_storage WHERE id = ?",
        )
        .bind(id)
        .fetch_optional(conn)
        .await?;
        Ok(row)
    }

    /// `UserStorageBo.exists` — whether the account user has subscribed to this
    /// universe.
    pub async fn exists(conn: &mut MySqlConnection, id: i32) -> OwgeResult<bool> {
        let count: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM user_storage WHERE id = ?")
            .bind(id)
            .fetch_one(&mut *conn)
            .await?;
        Ok(count > 0)
    }

    /// `UserStorageRepository.isBanned`.
    pub async fn is_banned(conn: &mut MySqlConnection, id: i32) -> OwgeResult<bool> {
        let banned: Option<i8> = sqlx::query_scalar("SELECT banned FROM user_storage WHERE id = ?")
            .bind(id)
            .fetch_optional(&mut *conn)
            .await?;
        Ok(matches!(banned, Some(b) if b != 0))
    }

    /// `UserStorageBo.triggerResourcesUpdate` — accrue the user's primary and
    /// secondary resources for the time elapsed since `last_action`, then stamp
    /// `last_action = now`.
    ///
    /// Java (`ResourceAutoUpdateEventHandler.doAfter`) runs this after auth on
    /// every authenticated game request. Per-second production for each resource
    /// is `faction.<x>_resource_production` boosted by the user's
    /// `more<X>ResourceProduction` improvement (`computePlusPercertage`:
    /// `base + base*(pct/100)`, computed in `float` like Java then widened); the
    /// delta is `elapsedSeconds * perSecond`.
    pub async fn trigger_resources_update(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<()> {
        // faction production rates + the user's last_action stamp.
        let row: Option<(Option<f32>, Option<f32>, chrono::NaiveDateTime)> = sqlx::query_as(
            "SELECT f.primary_resource_production, f.secondary_resource_production, us.last_action \
               FROM user_storage us JOIN factions f ON f.id = us.faction \
              WHERE us.id = ?",
        )
        .bind(user_id)
        .fetch_optional(&mut *conn)
        .await?;
        let Some((primary_prod, secondary_prod, last_action)) = row else {
            return Ok(());
        };

        let improvements = UserImprovementBo::find_user_improvement(&mut *conn, user_id).await?;
        let now = chrono::Utc::now().naive_utc();
        // calculateSum: (now - lastAction) in seconds (millis / 1000.0).
        let elapsed_secs = (now - last_action).num_milliseconds() as f64 / 1000.0;

        let primary_per_sec = compute_plus_percentage(
            primary_prod.unwrap_or(0.0),
            improvements.more_primary_resource_production as f32,
        );
        let secondary_per_sec = compute_plus_percentage(
            secondary_prod.unwrap_or(0.0),
            improvements.more_secondary_resource_production as f32,
        );
        let primary_delta = elapsed_secs * primary_per_sec;
        let secondary_delta = elapsed_secs * secondary_per_sec;

        // UserStorageRepository.addResources: lastAction = now, primary += , secondary += .
        sqlx::query(
            "UPDATE user_storage \
                SET last_action = ?, \
                    primary_resource = primary_resource + ?, \
                    secondary_resource = secondary_resource + ? \
              WHERE id = ?",
        )
        .bind(now)
        .bind(primary_delta)
        .bind(secondary_delta)
        .bind(user_id)
        .execute(&mut *conn)
        .await?;
        Ok(())
    }

    /// `UserDeleteService.deleteAccount` — the admin "delete user" cascade.
    ///
    /// Java fires every `UserDeleteListener.doDeleteUser(user)` in ascending
    /// `order()`, then deletes the `user_storage` row (the trailing websocket
    /// `account_deleted` emit is M4 — skipped). We replicate each listener's
    /// row mutations as raw SQL, ordered by the same resolved `order()`
    /// sequence, all inside a single transaction.
    ///
    /// Resolved order() values:
    ///   0 — UnlockedRelationBo, PlanetListBo, ObtainedUpgradeBo,
    ///       WebsocketEventsInformationBo, AuditMultiAccountSuspicionsService,
    ///       AuditBo, ActiveTimeSpecialBo
    ///   1 — AllianceJoinRequestBo
    ///   2 — AllianceBo, MissionReportBo
    ///   3 — ObtainedUnitBo, MissionBo
    ///   4 — UnitMissionBo, PlanetBo
    ///
    /// Within a tie, statements are emitted in an FK-safe order (e.g. suspicions
    /// before audit; obtained_units before missions). The home-planet load is
    /// read first so PlanetBo can clear its `home` flag.
    pub async fn delete_account(conn: &mut MySqlConnection, user_id: i32) -> OwgeResult<()> {
        // PlanetBo needs the user's home planet id to clear its `home` flag.
        let home_planet: Option<u64> =
            sqlx::query_scalar("SELECT home_planet FROM user_storage WHERE id = ?")
                .bind(user_id)
                .fetch_optional(&mut *conn)
                .await?;
        let Some(home_planet) = home_planet else {
            return Err(OwgeError::NotFound(format!("No user with id {user_id}")));
        };

        let mut tx = conn.begin().await?;

        // === order 0 ===

        // UnlockedRelationBo.doDeleteUser (order 0): repository.deleteByUser(user)
        sqlx::query("DELETE FROM unlocked_relation WHERE user_id = ?")
            .bind(user_id)
            .execute(&mut *tx)
            .await?;

        // PlanetListBo.doDeleteUser (order 0): repository.deleteByPlanetUserUser(user)
        // -> planet_list rows whose embedded PlanetUser.user matches (column user_id).
        sqlx::query("DELETE FROM planet_list WHERE user_id = ?")
            .bind(user_id)
            .execute(&mut *tx)
            .await?;

        // ObtainedUpgradeBo.doDeleteUser (order 0): obtainedUpgradeRepository.deleteByUser(user)
        sqlx::query("DELETE FROM obtained_upgrades WHERE user_id = ?")
            .bind(user_id)
            .execute(&mut *tx)
            .await?;

        // WebsocketEventsInformationBo.doDeleteUser (order 0):
        // repository.deleteByEventNameUserIdUserId(user.getId()) -> embedded id's user_id column.
        sqlx::query("DELETE FROM websocket_events_information WHERE user_id = ?")
            .bind(user_id)
            .execute(&mut *tx)
            .await?;

        // AuditMultiAccountSuspicionsService.doDeleteUser (order 0):
        // suspicionRepository.deleteByRelatedUser(user). The Suspicion entity maps
        // `relatedUser` to the `user_id` column. Deleted before `audit` because
        // suspicions.audit_id points at audit rows.
        sqlx::query("DELETE FROM suspicions WHERE user_id = ?")
            .bind(user_id)
            .execute(&mut *tx)
            .await?;

        // AuditBo.doDeleteUser (order 0): repository.deleteByUserOrRelatedUser(user, user).
        sqlx::query("DELETE FROM audit WHERE user_id = ? OR related_user_id = ?")
            .bind(user_id)
            .bind(user_id)
            .execute(&mut *tx)
            .await?;

        // ActiveTimeSpecialBo.doDeleteUser (order 0): repository.deleteByUser(user)
        sqlx::query("DELETE FROM active_time_specials WHERE user_id = ?")
            .bind(user_id)
            .execute(&mut *tx)
            .await?;

        // === order 1 ===

        // AllianceJoinRequestBo.doDeleteUser (order 1): if the user does not own
        // their own alliance, delete their join requests. (If they own the alliance,
        // AllianceBo at order 2 removes the alliance and its requests instead.)
        // user.getAlliance() == null || !user.getAlliance().getOwner().equals(user)
        let owns_own_alliance: bool = sqlx::query_scalar(
            "SELECT EXISTS( \
                SELECT 1 FROM user_storage u \
                JOIN alliances a ON a.id = u.alliance_id \
                WHERE u.id = ? AND a.owner_id = u.id)",
        )
        .bind(user_id)
        .fetch_one(&mut *tx)
        .await?;
        if !owns_own_alliance {
            sqlx::query("DELETE FROM alliance_join_request WHERE user_id = ?")
                .bind(user_id)
                .execute(&mut *tx)
                .await?;
        }

        // === order 2 ===

        // AllianceBo.doDeleteUser (order 2): if the user owns an alliance, reassign
        // its members (alliance_id -> NULL), drop its join requests, delete it.
        let owned_alliance_id: Option<u16> =
            sqlx::query_scalar("SELECT id FROM alliances WHERE owner_id = ?")
                .bind(user_id)
                .fetch_optional(&mut *tx)
                .await?;
        if let Some(alliance_id) = owned_alliance_id {
            // userStorageRepository.defineAllianceByAllianceId(alliance, null)
            sqlx::query("UPDATE user_storage SET alliance_id = NULL WHERE alliance_id = ?")
                .bind(alliance_id)
                .execute(&mut *tx)
                .await?;
            // allianceJoinRequestRepository.deleteByAlliance(alliance)
            sqlx::query("DELETE FROM alliance_join_request WHERE alliance_id = ?")
                .bind(alliance_id)
                .execute(&mut *tx)
                .await?;
            // repository.delete(alliance)
            sqlx::query("DELETE FROM alliances WHERE id = ?")
                .bind(alliance_id)
                .execute(&mut *tx)
                .await?;
        }

        // MissionReportBo.doDeleteUser (order 2):
        // missionRepository.updateReportId(null) -> UPDATE Mission SET report = NULL (all rows),
        // then missionReportRepository.deleteByUser(user).
        sqlx::query("UPDATE missions SET report_id = NULL")
            .execute(&mut *tx)
            .await?;
        sqlx::query("DELETE FROM mission_reports WHERE user_id = ?")
            .bind(user_id)
            .execute(&mut *tx)
            .await?;

        // === order 3 ===

        // ObtainedUnitBo.doDeleteUser (order 3): repository.deleteByUser(user).
        // Done before the mission deletes below: obtained_units.first_deployment_mission
        // is an FK to missions(id).
        sqlx::query("DELETE FROM obtained_units WHERE user_id = ?")
            .bind(user_id)
            .execute(&mut *tx)
            .await?;

        // MissionBo.doDeleteUser (order 3):
        // missionRepository.deleteByUserAndTypeCodeIn(user, [LEVEL_UP, BUILD_UNIT]).
        sqlx::query(
            "DELETE FROM missions \
             WHERE user_id = ? \
               AND type IN (SELECT id FROM mission_types WHERE code IN ('LEVEL_UP', 'BUILD_UNIT'))",
        )
        .bind(user_id)
        .execute(&mut *tx)
        .await?;

        // === order 4 ===

        // UnitMissionBo.doDeleteUser (order 4): delete the user's missions whose
        // type is a "unit mission" (MissionType.isUnitMission(): values 4..=11,
        // i.e. EXPLORE, RETURN_MISSION, GATHER, ESTABLISH_BASE, ATTACK,
        // COUNTERATTACK, CONQUEST, DEPLOY). Per-planet locking + websocket emit
        // are omitted (M4).
        sqlx::query(
            "DELETE FROM missions \
             WHERE user_id = ? \
               AND type IN (SELECT id FROM mission_types WHERE code IN \
                   ('EXPLORE', 'RETURN_MISSION', 'GATHER', 'ESTABLISH_BASE', \
                    'ATTACK', 'COUNTERATTACK', 'CONQUEST', 'DEPLOY'))",
        )
        .bind(user_id)
        .execute(&mut *tx)
        .await?;

        // PlanetBo.doDeleteUser (order 4): clear the home flag on the home planet,
        // release ownership of every planet the user owns, drop explored-planet rows.
        // explored_planets keys the user with column `user` (not `user_id`).
        sqlx::query("UPDATE planets SET home = 0 WHERE id = ?")
            .bind(home_planet)
            .execute(&mut *tx)
            .await?;
        sqlx::query("UPDATE planets SET owner = NULL WHERE owner = ?")
            .bind(user_id)
            .execute(&mut *tx)
            .await?;
        sqlx::query("DELETE FROM explored_planets WHERE user = ?")
            .bind(user_id)
            .execute(&mut *tx)
            .await?;

        // UserDeleteService.deleteAccount: finally delete the player record.
        sqlx::query("DELETE FROM user_storage WHERE id = ?")
            .bind(user_id)
            .execute(&mut *tx)
            .await?;

        tx.commit().await?;
        // socketIoService.sendOneTimeMessage(id, "account_deleted", () -> null) —
        // post-commit one-time push so the deleted user's clients log out.
        crate::bo::realtime_emitter::send_account_deleted(user_id).await?;
        Ok(())
    }

    /// `AdminGameUsersRestService.findById` (`GET admin/users/{id}`).
    pub async fn find_simple_by_id(
        conn: &mut MySqlConnection,
        id: i32,
    ) -> OwgeResult<SimpleUserData> {
        Ok(SimpleUserData::find_by_id(&mut *conn, &id).await?)
    }

    /// The `user_data_change` payload (`UserEventEmitterBo.findData`). Mirrors
    /// Java: `consumedEnergy = Σ(count × unit.energy)` and `maxEnergy =
    /// computeImprovementValue(faction.initialEnergy, moreEnergyProduction)`. The
    /// stored `user_storage.energy` is the *initial* energy, NOT the max (each
    /// faction has its own `initial_energy`). The `computed*` fields are never set
    /// by Java's `findData`, so they stay `None` (omitted from the JSON).
    pub async fn find_data(conn: &mut MySqlConnection, id: i32) -> OwgeResult<Option<UserData>> {
        Self::find_data_impl(conn, id, false).await
    }

    /// `find_data` for the SOCKET-pushed `user_data_change`: identical except
    /// `improvements.unitTypesUpgrades[].unitType` carries its own hydrated
    /// `speedImpactGroup` — Java's socket frames have it while the REST
    /// websocket-sync response does not (same lazy-init path-dependence as
    /// `user_improvements_change`; verified against captured frames, bdd run
    /// 20260709_231926).
    pub async fn find_data_for_socket(
        conn: &mut MySqlConnection,
        id: i32,
    ) -> OwgeResult<Option<UserData>> {
        Self::find_data_impl(conn, id, true).await
    }

    async fn find_data_impl(
        conn: &mut MySqlConnection,
        id: i32,
        for_socket: bool,
    ) -> OwgeResult<Option<UserData>> {
        let Some(u) = Self::find_by_id(&mut *conn, id).await? else {
            return Ok(None);
        };
        let improvement = UserImprovementBo::find_user_improvement(&mut *conn, id).await?;
        let initial_energy: Option<u32> =
            sqlx::query_scalar("SELECT initial_energy FROM factions WHERE id = ?")
                .bind(u.faction)
                .fetch_optional(&mut *conn)
                .await?;

        let faction = FactionBo::find_by_id_or_die(&mut *conn, u.faction).await?;

        let max_energy = crate::bo::mission_bo::compute_improvement_value(
            &mut *conn,
            initial_energy.unwrap_or(0) as f64,
            improvement.more_energy_production,
            true,
        )
        .await?;

        // Σ(count × energy) is integer-valued; CAST so sqlx decodes i64 (SUM → DECIMAL).
        let consumed: i64 = sqlx::query_scalar(
            "SELECT CAST(COALESCE(SUM(ou.count * un.energy), 0) AS SIGNED) \
               FROM obtained_units ou JOIN units un ON un.id = ou.unit_id WHERE ou.user_id = ?",
        )
        .bind(id)
        .fetch_one(&mut *conn)
        .await?;
        Ok(Some(UserData {
            can_alter_twitch_state: u.can_alter_twitch_state,
            consumed_energy: consumed as f64,
            email: u.email,
            faction,
            has_skipped_tutorial: u.has_skipped_tutorial,
            home_planet: PlanetBo::find_by_id_or_die(&mut *conn, u.home_planet).await?,
            id: u.id,
            improvements: if for_socket {
                UserImprovementBo::find_user_improvement_response_for_socket(&mut *conn, id).await?
            } else {
                UserImprovementBo::find_user_improvement_response(&mut *conn, id).await?
            },
            max_energy,
            primary_resource: u.primary_resource,
            secondary_resource: u.secondary_resource,
            username: u.username,
        }))
    }
}

/// `ImprovementBo.computePlusPercertage(Float base, Float percentage)` —
/// `base + base*(percentage/100)`, computed in `f32` (Java does the arithmetic
/// in `float` before widening to `double`). A null/zero percentage yields `base`.
fn compute_plus_percentage(base: f32, percentage: f32) -> f64 {
    (base + base * (percentage / 100.0_f32)) as f64
}
