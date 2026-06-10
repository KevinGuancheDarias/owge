//! Port of `AllianceBo` + `AllianceJoinRequestBo` (the controller-reachable
//! paths only). The two Java Bos are folded into one Rust module: the Java
//! `AllianceJoinRequestBo.save` (with its duplicate-check) is dead code on the
//! `AllianceRestService` path — `requestJoin` inserts directly via the
//! repository — so only the reachable behaviour is ported.
//!
//! Auditing (`auditBo.doAudit` / `nonRequestAudit`) is dropped (not ported); the
//! live port disables it, so it is intentionally a no-op. Websocket emissions are M4;
//! the Java code does not emit on these paths, so there are none to defer here.

use crate::dto::{AllianceDto, AllianceJoinRequestDto, SimpleUserData};
use crate::error::{OwgeError, OwgeResult};
use crate::model::Alliance;
use sqlx::{Connection, MySqlConnection};

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

const ALLIANCE_COLS: &str = "id, name, description, image, owner_id";

impl AllianceBo {
    /// `AllianceRestService.findAll` -> `allianceRepository.findAll()`.
    pub async fn find_all(conn: &mut MySqlConnection) -> OwgeResult<Vec<AllianceDto>> {
        let rows = sqlx::query_as::<_, AllianceRow>(
            "SELECT id, name, description, image, owner_id FROM alliances ORDER BY id",
        )
        .fetch_all(&mut *conn)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    async fn find_by_id_opt(conn: &mut MySqlConnection, id: u16) -> OwgeResult<Option<Alliance>> {
        let row = sqlx::query_as::<_, Alliance>(&format!(
            "SELECT {ALLIANCE_COLS} FROM alliances WHERE id = ?"
        ))
        .bind(id)
        .fetch_optional(&mut *conn)
        .await?;
        Ok(row)
    }

    async fn find_by_id_or_die(conn: &mut MySqlConnection, id: u16) -> OwgeResult<Alliance> {
        Self::find_by_id_opt(&mut *conn, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No alliance with id {id}")))
    }

    /// `AllianceRestService.members` -> `AllianceRepository.findMembers` — the
    /// users whose `alliance_id` is this alliance. Email is blanked and
    /// improvements omitted (the Rust DTO has no improvements field), matching
    /// the Java controller's `setEmail(null); setImprovements(null)`.
    pub async fn members(
        conn: &mut MySqlConnection,
        alliance_id: u16,
    ) -> OwgeResult<Vec<SimpleUserData>> {
        let rows = SimpleUserData::builder_select()
            .id(&4)?
            .alliance_id(&Some(alliance_id))?
            .find_all(&mut *conn)
            .await?;

        Ok(rows)
    }

    /// `AllianceBo.save(alliance, invokerId)` — create (id null) or update
    /// (id present). Runs in a transaction so the create path's two writes
    /// (insert alliance + point owner at it) are atomic.
    pub async fn save(
        conn: &mut MySqlConnection,
        dto: AllianceDto,
        invoker_id: i32,
    ) -> OwgeResult<AllianceDto> {
        let disabled =
            ConfigurationBo::find_or_set_default(&mut *conn, "DISABLED_FEATURE_ALLIANCE", "FALSE")
                .await?
                .value;
        if disabled.eq_ignore_ascii_case("true") {
            return Err(OwgeError::InvalidInput(
                "You can't not create an alliance, while the idea is nice, it's not possible"
                    .into(),
            ));
        }

        // On the Rust side the create path is distinguished by id == 0 (no body
        // id sent). The frontend omits/zeroes id when creating.
        let is_create = dto.id == 0;

        if is_create {
            let creator = SimpleUserData::find_by_id(&mut *conn, &invoker_id).await?;

            if creator.alliance_id.is_some() {
                return Err(OwgeError::InvalidInput(
                    "You already have an alliance, leave it first".into(),
                ));
            }

            let mut tx = conn.begin().await?;
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
            let stored = Self::find_by_id_or_die(&mut *conn, dto.id).await?;
            Self::check_invoker_is_owner(&stored, invoker_id)?;
            // Update only name + description (matching Java).
            sqlx::query("UPDATE alliances SET name = ?, description = ? WHERE id = ?")
                .bind(&dto.name)
                .bind(&dto.description)
                .bind(stored.id)
                .execute(&mut *conn)
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
    pub async fn delete_by_user(conn: &mut MySqlConnection, invoker_id: i32) -> OwgeResult<()> {
        let user = SimpleUserData::find_by_id(&mut *conn, &invoker_id).await?;

        let alliance_id = user
            .alliance_id
            .ok_or_else(|| OwgeError::InvalidInput("You don't have any alliance".into()))?;

        let alliance = Self::find_by_id_or_die(&mut *conn, alliance_id).await?;

        Self::check_invoker_is_owner(&alliance, invoker_id)?;

        let mut tx = conn.begin().await?;

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
    pub async fn list_request(
        conn: &mut MySqlConnection,
        invoker_id: i32,
    ) -> OwgeResult<Vec<AllianceJoinRequestDto>> {
        let user = SimpleUserData::find_by_id(&mut *conn, &invoker_id).await?;

        let alliance_id = user.require_alliance()?;

        let alliance = Self::find_by_id_or_die(&mut *conn, alliance_id).await?;
        alliance.check_owner(invoker_id)?;

        Self::find_join_request_dtos(&mut *conn, "alliance_id", alliance_id as i64).await
    }

    /// `AllianceRestService.myRequests` -> `findByUserId`.
    pub async fn my_requests(
        conn: &mut MySqlConnection,
        invoker_id: i32,
    ) -> OwgeResult<Vec<AllianceJoinRequestDto>> {
        Self::find_join_request_dtos(&mut *conn, "user_id", invoker_id as i64).await
    }

    /// `AllianceRestService.myRequestsDelete` -> bare `deleteById` (no checks,
    /// matching Java).
    pub async fn delete_join_request_by_id(conn: &mut MySqlConnection, id: u32) -> OwgeResult<()> {
        sqlx::query("DELETE FROM alliance_join_request WHERE id = ?")
            .bind(id)
            .execute(&mut *conn)
            .await?;
        Ok(())
    }

    /// `AllianceBo.requestJoin` — create a join request. The Java path inserts
    /// directly via the repository (no duplicate check), so we replicate that
    /// and set `request_date = now (UTC)` since the column is NOT NULL.
    pub async fn request_join(
        conn: &mut MySqlConnection,
        alliance_id: u16,
        invoker_id: i32,
    ) -> OwgeResult<AllianceJoinRequestDto> {
        let alliance = Self::find_by_id_or_die(&mut *conn, alliance_id).await?;
        let user = SimpleUserData::find_by_id(&mut *conn, &invoker_id).await?;

        user.check_no_alliance()?;

        let now = chrono::Utc::now().naive_utc();
        let result = sqlx::query(
            "INSERT INTO alliance_join_request (alliance_id, user_id, request_date) VALUES (?, ?, ?)",
        )
        .bind(alliance_id)
        .bind(invoker_id)
        .bind(now)
        .execute(&mut *conn)
        .await?;
        let new_id = result.last_insert_id() as u32;
        Ok(AllianceJoinRequestDto {
            id: new_id,
            user,
            alliance: alliance.into(),
        })
    }

    /// `AllianceBo.acceptJoin` — accept a pending request: the invoker must own
    /// the request's alliance, the alliance must not be full, and if the
    /// requesting user has no alliance their `alliance_id` is set and all of
    /// their join requests are removed; otherwise just this request is removed.
    pub async fn accept_join(
        conn: &mut MySqlConnection,
        join_request_id: u32,
        invoker_id: i32,
    ) -> OwgeResult<()> {
        let request = Self::find_join_request_or_die(&mut *conn, join_request_id).await?;
        let alliance = Self::find_by_id_or_die(&mut *conn, request.alliance_id).await?;
        Self::check_invoker_is_owner(&alliance, invoker_id)?;
        Self::check_is_limit_reached(&mut *conn, request.alliance_id).await?;

        let requesting_user = SimpleUserData::find_by_id(&mut *conn, &request.user_id).await?;

        let mut tx = conn.begin().await?;

        if requesting_user.alliance_id.is_none() {
            sqlx::query("UPDATE user_storage SET alliance_id = ? WHERE id = ?")
                .bind(request.alliance_id)
                .bind(request.user_id)
                .execute(&mut *tx)
                .await?;

            sqlx::query("DELETE FROM alliance_join_request WHERE user_id = ?")
                .bind(request.user_id)
                .execute(&mut *tx)
                .await?;
        } else {
            // If accepts but the user already has an alliance, remove its join request
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
    pub async fn reject_join(
        conn: &mut MySqlConnection,
        join_request_id: u32,
        invoker_id: i32,
    ) -> OwgeResult<()> {
        let request = Self::find_join_request_or_die(&mut *conn, join_request_id).await?;
        let alliance = Self::find_by_id_or_die(&mut *conn, request.alliance_id).await?;
        Self::check_invoker_is_owner(&alliance, invoker_id)?;
        sqlx::query("DELETE FROM alliance_join_request WHERE id = ?")
            .bind(join_request_id)
            .execute(&mut *conn)
            .await?;
        Ok(())
    }

    /// `AllianceBo.leave` — owners can't leave their own alliance; otherwise
    /// unset the invoker's `alliance_id`.
    pub async fn leave(conn: &mut MySqlConnection, invoker_id: i32) -> OwgeResult<()> {
        let owns: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM alliances WHERE owner_id = ?")
            .bind(invoker_id)
            .fetch_one(&mut *conn)
            .await?;
        if owns > 0 {
            return Err(OwgeError::InvalidInput(
                "You can't leave your own alliance".into(),
            ));
        }
        sqlx::query("UPDATE user_storage SET alliance_id = NULL WHERE id = ?")
            .bind(invoker_id)
            .execute(&mut *conn)
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
    async fn check_is_limit_reached(
        conn: &mut MySqlConnection,
        alliance_id: u16,
    ) -> OwgeResult<()> {
        let user_count: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM user_storage")
            .fetch_one(&mut *conn)
            .await?;
        let allowed_percentage: i64 =
            ConfigurationBo::find_or_set_default(&mut *conn, "ALLIANCE_MAX_SIZE_PERCENTAGE", "7")
                .await?
                .value
                .trim()
                .parse()
                .unwrap_or(0);
        let max: i64 = ConfigurationBo::find_or_set_default(&mut *conn, "ALLIANCE_MAX_SIZE", "15")
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
                .fetch_one(&mut *conn)
                .await?;
        if max_allowed <= in_alliance {
            return Err(OwgeError::InvalidInput("I18N_ERR_ALLIANCE_IS_FULL".into()));
        }
        Ok(())
    }

    async fn find_join_request_or_die(
        conn: &mut MySqlConnection,
        id: u32,
    ) -> OwgeResult<crate::model::AllianceJoinRequest> {
        let row = sqlx::query_as::<_, crate::model::AllianceJoinRequest>(
            "SELECT id, alliance_id, user_id, request_date FROM alliance_join_request WHERE id = ?",
        )
        .bind(id)
        .fetch_optional(&mut *conn)
        .await?;
        row.ok_or_else(|| OwgeError::NotFound(format!("No join request with id {id}")))
    }

    /// Loads join requests filtered on `column = value` (column is a fixed
    /// internal literal, never user input), eager-joining the user and alliance
    /// into full DTOs.
    async fn find_join_request_dtos(
        conn: &mut MySqlConnection,
        column: &str,
        value: i64,
    ) -> OwgeResult<Vec<AllianceJoinRequestDto>> {
        let requests = sqlx::query_as::<_, crate::model::AllianceJoinRequest>(&format!(
            "SELECT id, alliance_id, user_id, request_date FROM alliance_join_request \
             WHERE {column} = ? ORDER BY id"
        ))
        .bind(value)
        .fetch_all(&mut *conn)
        .await?;

        let mut out = Vec::with_capacity(requests.len());
        for req in requests {
            let user = SimpleUserData::find_by_id(&mut *conn, &req.user_id).await?;

            let alliance = Self::find_by_id_or_die(&mut *conn, req.alliance_id).await?;
            out.push(AllianceJoinRequestDto {
                id: req.id,
                user,
                alliance: alliance.into(),
            });
        }
        Ok(out)
    }
}
