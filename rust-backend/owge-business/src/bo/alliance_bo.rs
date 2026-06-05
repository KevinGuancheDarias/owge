//! Port of `AllianceBo` + `AllianceJoinRequestBo` (the controller-reachable
//! paths only). The two Java Bos are folded into one Rust module: the Java
//! `AllianceJoinRequestBo.save` (with its duplicate-check) is dead code on the
//! `AllianceRestService` path — `requestJoin` inserts directly via the
//! repository — so only the reachable behaviour is ported.
//!
//! Auditing (`auditBo.doAudit` / `nonRequestAudit`) is dropped (not ported); the
//! live port disables it, so it is intentionally a no-op. Websocket emissions are M4;
//! the Java code does not emit on these paths, so there are none to defer here.

use crate::db::Db;
use crate::dto::{AllianceDto, AllianceJoinRequestDto, UserStorageDto};
use crate::error::{OwgeError, OwgeResult};
use crate::model::{Alliance, UserStorage};

use super::ConfigurationBo;

pub struct AllianceBo;

/// `alliances` row joined with nothing — straight column mapping.
#[derive(sqlx::FromRow)]
struct AllianceRow {
    id: u16,
    name: String,
    description: Option<String>,
    image: Option<String>,
    owner_id: i32,
}

impl From<AllianceRow> for AllianceDto {
    fn from(r: AllianceRow) -> Self {
        AllianceDto {
            id: r.id,
            name: r.name,
            description: r.description,
            image: r.image,
            owner: r.owner_id,
        }
    }
}

impl From<Alliance> for AllianceDto {
    fn from(a: Alliance) -> Self {
        AllianceDto {
            id: a.id,
            name: a.name,
            description: a.description,
            image: a.image,
            owner: a.owner_id,
        }
    }
}

/// Builds the (simple) `UserStorageDto` projection used by the alliance member
/// list and the join-request payloads. Mirrors `UserStorageDto.dtoFromEntity`
/// for the fields the Rust DTO carries (nested faction/home-planet/alliance and
/// `improvements` are not part of the Rust DTO yet); `maxEnergy` is nulled like
/// Java. Computed values mirror stored values until the improvement engine is
/// wired.
fn user_dto_from(u: UserStorage) -> UserStorageDto {
    UserStorageDto {
        id: u.id,
        username: u.username,
        email: u.email,
        primary_resource: u.primary_resource,
        secondary_resource: u.secondary_resource,
        consumed_energy: Some(u.energy),
        primary_resource_generation_per_second: u.primary_resource_generation_per_second,
        secondary_resource_generation_per_second: u.secondary_resource_generation_per_second,
        max_energy: None,
        has_skipped_tutorial: u.has_skipped_tutorial,
        can_alter_twitch_state: u.can_alter_twitch_state,
        computed_primary_resource_generation_per_second: u.primary_resource_generation_per_second,
        computed_secondary_resource_generation_per_second: u
            .secondary_resource_generation_per_second,
        computed_max_energy: None,
    }
}

const USER_COLS: &str = "id, username, email, alliance_id, faction, last_action, home_planet, \
    primary_resource, secondary_resource, energy, \
    primary_resource_generation_per_second, secondary_resource_generation_per_second, \
    has_skipped_tutorial, points, can_alter_twitch_state, banned";

const ALLIANCE_COLS: &str = "id, name, description, image, owner_id";

impl AllianceBo {
    /// `AllianceRestService.findAll` -> `allianceRepository.findAll()`.
    pub async fn find_all(db: &Db) -> OwgeResult<Vec<AllianceDto>> {
        let rows = sqlx::query_as::<_, AllianceRow>(
            "SELECT id, name, description, image, owner_id FROM alliances ORDER BY id",
        )
        .fetch_all(db)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    async fn find_by_id_opt(db: &Db, id: u16) -> OwgeResult<Option<Alliance>> {
        let row = sqlx::query_as::<_, Alliance>(&format!(
            "SELECT {ALLIANCE_COLS} FROM alliances WHERE id = ?"
        ))
        .bind(id)
        .fetch_optional(db)
        .await?;
        Ok(row)
    }

    async fn find_by_id_or_die(db: &Db, id: u16) -> OwgeResult<Alliance> {
        Self::find_by_id_opt(db, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No alliance with id {id}")))
    }

    /// `AllianceRestService.members` -> `AllianceRepository.findMembers` — the
    /// users whose `alliance_id` is this alliance. Email is blanked and
    /// improvements omitted (the Rust DTO has no improvements field), matching
    /// the Java controller's `setEmail(null); setImprovements(null)`.
    pub async fn members(db: &Db, alliance_id: u16) -> OwgeResult<Vec<UserStorageDto>> {
        let rows = sqlx::query_as::<_, UserStorage>(&format!(
            "SELECT {USER_COLS} FROM user_storage WHERE alliance_id = ? ORDER BY id"
        ))
        .bind(alliance_id)
        .fetch_all(db)
        .await?;
        Ok(rows
            .into_iter()
            .map(|u| {
                let mut dto = user_dto_from(u);
                // Java nulls email; the Rust DTO field is non-optional, so blank
                // it (never leak member emails).
                dto.email = String::new();
                dto
            })
            .collect())
    }

    /// `AllianceBo.save(alliance, invokerId)` — create (id null) or update
    /// (id present). Runs in a transaction so the create path's two writes
    /// (insert alliance + point owner at it) are atomic.
    pub async fn save(db: &Db, dto: AllianceDto, invoker_id: i32) -> OwgeResult<AllianceDto> {
        let disabled = ConfigurationBo::find_or_set_default(db, "DISABLED_FEATURE_ALLIANCE", "FALSE")
            .await?
            .value;
        if disabled.eq_ignore_ascii_case("true") {
            return Err(OwgeError::InvalidInput(
                "You can't not create an alliance, while the idea is nice, it's not possible".into(),
            ));
        }

        // On the Rust side the create path is distinguished by id == 0 (no body
        // id sent). The frontend omits/zeroes id when creating.
        let is_create = dto.id == 0;

        if is_create {
            let creator = sqlx::query_as::<_, UserStorage>(&format!(
                "SELECT {USER_COLS} FROM user_storage WHERE id = ?"
            ))
            .bind(invoker_id)
            .fetch_optional(db)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No user with id {invoker_id}")))?;
            if creator.alliance_id.is_some() {
                return Err(OwgeError::InvalidInput(
                    "You already have an alliance, leave it first".into(),
                ));
            }

            let mut tx = db.begin().await?;
            let result = sqlx::query(
                "INSERT INTO alliances (name, description, image, owner_id) VALUES (?, ?, ?, ?)",
            )
            .bind(&dto.name)
            .bind(&dto.description)
            .bind(&dto.image)
            .bind(invoker_id)
            .execute(&mut *tx)
            .await?;
            let new_id = result.last_insert_id() as u16;

            sqlx::query("UPDATE user_storage SET alliance_id = ? WHERE id = ?")
                .bind(new_id)
                .bind(invoker_id)
                .execute(&mut *tx)
                .await?;
            tx.commit().await?;
            // auditBo.doAudit(JOIN_ALLIANCE) — NOT PORTED (auditing disabled in the
            // live port and intentionally not ported.

            Ok(AllianceDto {
                id: new_id,
                name: dto.name,
                description: dto.description,
                image: dto.image,
                owner: invoker_id,
            })
        } else {
            let stored = Self::find_by_id_or_die(db, dto.id).await?;
            Self::check_invoker_is_owner(&stored, invoker_id)?;
            // Update only name + description (matching Java).
            sqlx::query("UPDATE alliances SET name = ?, description = ? WHERE id = ?")
                .bind(&dto.name)
                .bind(&dto.description)
                .bind(stored.id)
                .execute(db)
                .await?;
            Ok(AllianceDto {
                id: stored.id,
                name: dto.name,
                description: dto.description,
                image: stored.image,
                owner: stored.owner_id,
            })
        }
    }

    /// `AllianceBo.deleteByUser` — delete the invoker's own alliance (they must
    /// own one). Unsets every member's `alliance_id`, deletes the alliance's
    /// join requests, then deletes the alliance row.
    pub async fn delete_by_user(db: &Db, invoker_id: i32) -> OwgeResult<()> {
        let user = sqlx::query_as::<_, UserStorage>(&format!(
            "SELECT {USER_COLS} FROM user_storage WHERE id = ?"
        ))
        .bind(invoker_id)
        .fetch_optional(db)
        .await?
        .ok_or_else(|| OwgeError::NotFound(format!("No user with id {invoker_id}")))?;
        let alliance_id = user
            .alliance_id
            .ok_or_else(|| OwgeError::InvalidInput("You don't have any alliance".into()))?;
        let alliance = Self::find_by_id_or_die(db, alliance_id).await?;
        Self::check_invoker_is_owner(&alliance, invoker_id)?;

        let mut tx = db.begin().await?;
        // defineAllianceByAllianceId(id, null): unset all members.
        sqlx::query("UPDATE user_storage SET alliance_id = NULL WHERE alliance_id = ?")
            .bind(alliance_id)
            .execute(&mut *tx)
            .await?;
        sqlx::query("DELETE FROM alliance_join_request WHERE alliance_id = ?")
            .bind(alliance_id)
            .execute(&mut *tx)
            .await?;
        sqlx::query("DELETE FROM alliances WHERE id = ?")
            .bind(alliance_id)
            .execute(&mut *tx)
            .await?;
        tx.commit().await?;
        Ok(())
    }

    /// `AllianceRestService.listRequest` — join requests for the invoker's
    /// alliance. The invoker must own an alliance.
    pub async fn list_request(db: &Db, invoker_id: i32) -> OwgeResult<Vec<AllianceJoinRequestDto>> {
        let user = sqlx::query_as::<_, UserStorage>(&format!(
            "SELECT {USER_COLS} FROM user_storage WHERE id = ?"
        ))
        .bind(invoker_id)
        .fetch_optional(db)
        .await?
        .ok_or_else(|| OwgeError::NotFound(format!("No user with id {invoker_id}")))?;
        let alliance_id = user
            .alliance_id
            .ok_or_else(|| OwgeError::InvalidInput("You don't have any alliance".into()))?;
        let alliance = Self::find_by_id_or_die(db, alliance_id).await?;
        if alliance.owner_id != invoker_id {
            return Err(OwgeError::InvalidInput(
                "You are not the owner of the alliance".into(),
            ));
        }
        Self::find_join_request_dtos(db, "alliance_id", alliance_id as i64).await
    }

    /// `AllianceRestService.myRequests` -> `findByUserId`.
    pub async fn my_requests(db: &Db, invoker_id: i32) -> OwgeResult<Vec<AllianceJoinRequestDto>> {
        Self::find_join_request_dtos(db, "user_id", invoker_id as i64).await
    }

    /// `AllianceRestService.myRequestsDelete` -> bare `deleteById` (no checks,
    /// matching Java).
    pub async fn delete_join_request_by_id(db: &Db, id: u32) -> OwgeResult<()> {
        sqlx::query("DELETE FROM alliance_join_request WHERE id = ?")
            .bind(id)
            .execute(db)
            .await?;
        Ok(())
    }

    /// `AllianceBo.requestJoin` — create a join request. The Java path inserts
    /// directly via the repository (no duplicate check), so we replicate that
    /// and set `request_date = now (UTC)` since the column is NOT NULL.
    pub async fn request_join(
        db: &Db,
        alliance_id: u16,
        invoker_id: i32,
    ) -> OwgeResult<AllianceJoinRequestDto> {
        let alliance = Self::find_by_id_or_die(db, alliance_id).await?;
        let user = sqlx::query_as::<_, UserStorage>(&format!(
            "SELECT {USER_COLS} FROM user_storage WHERE id = ?"
        ))
        .bind(invoker_id)
        .fetch_optional(db)
        .await?
        .ok_or_else(|| OwgeError::NotFound(format!("No user with id {invoker_id}")))?;
        if user.alliance_id.is_some() {
            return Err(OwgeError::InvalidInput(
                "You are already in an alliance, nice try!".into(),
            ));
        }
        let now = chrono::Utc::now().naive_utc();
        let result = sqlx::query(
            "INSERT INTO alliance_join_request (alliance_id, user_id, request_date) VALUES (?, ?, ?)",
        )
        .bind(alliance_id)
        .bind(invoker_id)
        .bind(now)
        .execute(db)
        .await?;
        let new_id = result.last_insert_id() as u32;
        Ok(AllianceJoinRequestDto {
            id: new_id,
            user: user_dto_from(user),
            alliance: alliance.into(),
        })
    }

    /// `AllianceBo.acceptJoin` — accept a pending request: the invoker must own
    /// the request's alliance, the alliance must not be full, and if the
    /// requesting user has no alliance their `alliance_id` is set and all of
    /// their join requests are removed; otherwise just this request is removed.
    pub async fn accept_join(db: &Db, join_request_id: u32, invoker_id: i32) -> OwgeResult<()> {
        let request = Self::find_join_request_or_die(db, join_request_id).await?;
        let alliance = Self::find_by_id_or_die(db, request.alliance_id).await?;
        Self::check_invoker_is_owner(&alliance, invoker_id)?;
        Self::check_is_limit_reached(db, request.alliance_id).await?;

        let requesting_user = sqlx::query_as::<_, UserStorage>(&format!(
            "SELECT {USER_COLS} FROM user_storage WHERE id = ?"
        ))
        .bind(request.user_id)
        .fetch_optional(db)
        .await?
        .ok_or_else(|| OwgeError::NotFound(format!("No user with id {}", request.user_id)))?;

        let mut tx = db.begin().await?;
        if requesting_user.alliance_id.is_none() {
            sqlx::query("UPDATE user_storage SET alliance_id = ? WHERE id = ?")
                .bind(request.alliance_id)
                .bind(request.user_id)
                .execute(&mut *tx)
                .await?;
            // (M5) auditBo audits — disabled in the live port, not ported.
            // deleteByUser: remove all of the user's join requests.
            sqlx::query("DELETE FROM alliance_join_request WHERE user_id = ?")
                .bind(request.user_id)
                .execute(&mut *tx)
                .await?;
        } else {
            sqlx::query("DELETE FROM alliance_join_request WHERE id = ?")
                .bind(join_request_id)
                .execute(&mut *tx)
                .await?;
        }
        tx.commit().await?;
        Ok(())
    }

    /// `AllianceBo.rejectJoin` — invoker must own the request's alliance; delete
    /// the request.
    pub async fn reject_join(db: &Db, join_request_id: u32, invoker_id: i32) -> OwgeResult<()> {
        let request = Self::find_join_request_or_die(db, join_request_id).await?;
        let alliance = Self::find_by_id_or_die(db, request.alliance_id).await?;
        Self::check_invoker_is_owner(&alliance, invoker_id)?;
        sqlx::query("DELETE FROM alliance_join_request WHERE id = ?")
            .bind(join_request_id)
            .execute(db)
            .await?;
        Ok(())
    }

    /// `AllianceBo.leave` — owners can't leave their own alliance; otherwise
    /// unset the invoker's `alliance_id`.
    pub async fn leave(db: &Db, invoker_id: i32) -> OwgeResult<()> {
        let owns: i64 =
            sqlx::query_scalar("SELECT COUNT(*) FROM alliances WHERE owner_id = ?")
                .bind(invoker_id)
                .fetch_one(db)
                .await?;
        if owns > 0 {
            return Err(OwgeError::InvalidInput(
                "You can't leave your own alliance".into(),
            ));
        }
        sqlx::query("UPDATE user_storage SET alliance_id = NULL WHERE id = ?")
            .bind(invoker_id)
            .execute(db)
            .await?;
        Ok(())
    }

    fn check_invoker_is_owner(alliance: &Alliance, invoker_id: i32) -> OwgeResult<()> {
        if alliance.owner_id != invoker_id {
            return Err(OwgeError::InvalidInput(
                "You are NOT the owner of that alliance, try hacking the owner account".into(),
            ));
        }
        Ok(())
    }

    /// `AllianceBo.checkIsLimitReached`.
    async fn check_is_limit_reached(db: &Db, alliance_id: u16) -> OwgeResult<()> {
        let user_count: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM user_storage")
            .fetch_one(db)
            .await?;
        let allowed_percentage: i64 =
            ConfigurationBo::find_or_set_default(db, "ALLIANCE_MAX_SIZE_PERCENTAGE", "7")
                .await?
                .value
                .trim()
                .parse()
                .unwrap_or(0);
        let max: i64 = ConfigurationBo::find_or_set_default(db, "ALLIANCE_MAX_SIZE", "15")
            .await?
            .value
            .trim()
            .parse()
            .unwrap_or(0);
        let mut allowed_by_percentage = user_count as f32 * (allowed_percentage as f32 / 100.0);
        if allowed_by_percentage < 2.0 {
            allowed_by_percentage = 2.0;
        }
        let max_allowed = if allowed_by_percentage < max as f32 {
            allowed_by_percentage as i64
        } else {
            max
        };
        let in_alliance: i64 =
            sqlx::query_scalar("SELECT COUNT(*) FROM user_storage WHERE alliance_id = ?")
                .bind(alliance_id)
                .fetch_one(db)
                .await?;
        if max_allowed <= in_alliance {
            return Err(OwgeError::InvalidInput("I18N_ERR_ALLIANCE_IS_FULL".into()));
        }
        Ok(())
    }

    async fn find_join_request_or_die(
        db: &Db,
        id: u32,
    ) -> OwgeResult<crate::model::AllianceJoinRequest> {
        let row = sqlx::query_as::<_, crate::model::AllianceJoinRequest>(
            "SELECT id, alliance_id, user_id, request_date FROM alliance_join_request WHERE id = ?",
        )
        .bind(id)
        .fetch_optional(db)
        .await?;
        row.ok_or_else(|| OwgeError::NotFound(format!("No join request with id {id}")))
    }

    /// Loads join requests filtered on `column = value` (column is a fixed
    /// internal literal, never user input), eager-joining the user and alliance
    /// into full DTOs.
    async fn find_join_request_dtos(
        db: &Db,
        column: &str,
        value: i64,
    ) -> OwgeResult<Vec<AllianceJoinRequestDto>> {
        let requests = sqlx::query_as::<_, crate::model::AllianceJoinRequest>(&format!(
            "SELECT id, alliance_id, user_id, request_date FROM alliance_join_request \
             WHERE {column} = ? ORDER BY id"
        ))
        .bind(value)
        .fetch_all(db)
        .await?;

        let mut out = Vec::with_capacity(requests.len());
        for req in requests {
            let user = sqlx::query_as::<_, UserStorage>(&format!(
                "SELECT {USER_COLS} FROM user_storage WHERE id = ?"
            ))
            .bind(req.user_id)
            .fetch_optional(db)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No user with id {}", req.user_id)))?;
            let alliance = Self::find_by_id_or_die(db, req.alliance_id).await?;
            out.push(AllianceJoinRequestDto {
                id: req.id,
                user: user_dto_from(user),
                alliance: alliance.into(),
            });
        }
        Ok(out)
    }
}
