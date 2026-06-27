//! Port of (the read side of) the time-special domain business objects:
//! `com.kevinguanchedarias.owgejava.business.TimeSpecialBo` and
//! `...business.ActiveTimeSpecialBo`.
//!
//! Direct reads back `GET game/time_special` (list) and the by-id read,
//! building `TimeSpecialDto`s with the image URL resolved via a join, the
//! per-user activation status (`activeTimeSpecialDto`) filled from
//! `active_time_specials`, and the `improvement` loaded from the improvement
//! engine (Java's `DtoWithImprovements`), matching `TimeSpecialBo.toDto`.
//!
//! The `time_special_change` sync payload
//! (`ActiveTimeSpecialBo.findByUserWithCurrentStatus`) only returns the time
//! specials the user has *unlocked* (`unlocked_relation` → `object_relations` of
//! type `TIME_SPECIAL`) — see [`TimeSpecialBo::find_user_status_dtos`].

use crate::bo::ImprovementBo;
use crate::dto::time_special::{ActiveTimeSpecialDto, TimeSpecialDto};
use crate::error::OwgeResult;
use sqlx::MySqlConnection;

/// A `time_specials` row joined with its image and (optionally) the requesting
/// user's `active_time_specials` row — exact SQL column types so sqlx never
/// panics on signedness/width.
#[derive(sqlx::FromRow)]
struct TimeSpecialRow {
    id: u16,
    name: String,
    description: Option<String>,
    image: Option<u64>,
    image_filename: Option<String>,
    duration: u64,
    recharge_time: u64,
    /// `improvement_id` FK; read by the `Bo` to load the nested `improvement`
    /// (the `From` impl is sync and cannot query, so it leaves it `None`).
    improvement_id: Option<u16>,

    // --- the user's active_time_specials row (all NULL when not active) ---
    active_id: Option<u64>,
    active_state: Option<String>,
    active_activation_date: Option<chrono::NaiveDateTime>,
    active_expiring_date: Option<chrono::NaiveDateTime>,
    active_ready_date: Option<chrono::NaiveDateTime>,
}

impl From<TimeSpecialRow> for TimeSpecialDto {
    fn from(r: TimeSpecialRow) -> Self {
        let image_url = r
            .image_filename
            .map(|f| crate::bo::image_store_bo::compute_image_url(&f));
        let active_time_special_dto = match (r.active_id, r.active_state) {
            (Some(id), Some(state)) => {
                let activation_date = r.active_activation_date.map(millis).unwrap_or_default();
                let expiring_date = r.active_expiring_date.map(millis).unwrap_or_default();
                let ready_date = r.active_ready_date.map(millis);
                // `ActiveTimeSpecialDto.calculatePendingMillis`: time left until
                // the effect ends (ACTIVE) or until ready again (RECHARGE).
                let target = if state == "ACTIVE" {
                    expiring_date
                } else {
                    ready_date.unwrap_or(expiring_date)
                };
                let pending_millis = target - now_millis();
                Some(ActiveTimeSpecialDto {
                    id,
                    time_special: r.id,
                    state,
                    activation_date,
                    expiring_date,
                    ready_date,
                    pending_millis,
                })
            }
            _ => None,
        };
        TimeSpecialDto {
            id: r.id,
            name: r.name,
            description: r.description,
            image: r.image,
            image_url,
            duration: r.duration,
            recharge_time: r.recharge_time,
            // Loaded by the async read methods (see `enrich_improvements`).
            improvement: None,
            active_time_special_dto,
        }
    }
}

fn millis(dt: chrono::NaiveDateTime) -> i64 {
    dt.and_utc().timestamp_millis()
}

fn now_millis() -> i64 {
    chrono::Utc::now().timestamp_millis()
}

/// Converts rows to DTOs, loading each one's `improvement` (Java's
/// `DtoWithImprovements.dtoFromEntity`). The `From` impl is sync and leaves
/// `improvement` `None`; this fills it from the improvement engine for any time
/// special that has an `improvement_id`.
async fn rows_to_dtos(
    conn: &mut MySqlConnection,
    rows: Vec<TimeSpecialRow>,
) -> OwgeResult<Vec<TimeSpecialDto>> {
    let mut dtos = Vec::with_capacity(rows.len());
    for row in rows {
        let improvement_id = row.improvement_id;
        let mut dto = TimeSpecialDto::from(row);
        if let Some(id) = improvement_id {
            dto.improvement = Some(ImprovementBo::find_dto(&mut *conn, Some(id)).await?);
        }
        dtos.push(dto);
    }
    Ok(dtos)
}

const SELECT_DTO: &str = "\
    SELECT ts.id, ts.name, ts.description, \
           ts.image_id AS image, i.filename AS image_filename, \
           ts.duration, ts.recharge_time AS recharge_time, \
           ts.improvement_id AS improvement_id, \
           ats.id AS active_id, ats.state AS active_state, \
           ats.activation_date AS active_activation_date, \
           ats.expiring_date AS active_expiring_date, \
           ats.ready_date AS active_ready_date \
    FROM time_specials ts \
    LEFT JOIN images_store i ON i.id = ts.image_id \
    LEFT JOIN active_time_specials ats \
           ON ats.time_special_id = ts.id AND ats.user_id = ? ";

pub struct TimeSpecialBo;

impl TimeSpecialBo {
    /// `GET game/time_special` — the read-CRUD list. Returns every time special
    /// with the requesting user's activation status filled in
    /// (`TimeSpecialBo.toDto`).
    pub async fn find_all_dtos(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<Vec<TimeSpecialDto>> {
        let rows = sqlx::query_as::<_, TimeSpecialRow>(&format!("{SELECT_DTO} ORDER BY ts.id"))
            .bind(user_id)
            .fetch_all(&mut *conn)
            .await?;
        rows_to_dtos(&mut *conn, rows).await
    }

    /// `GET game/time_special/{id}` — a single time special with the requesting
    /// user's activation status, or `None` when it does not exist.
    pub async fn find_dto_by_id(
        conn: &mut MySqlConnection,
        user_id: i32,
        id: u16,
    ) -> OwgeResult<Option<TimeSpecialDto>> {
        let row = sqlx::query_as::<_, TimeSpecialRow>(&format!("{SELECT_DTO} WHERE ts.id = ?"))
            .bind(user_id)
            .bind(id)
            .fetch_optional(&mut *conn)
            .await?;
        match row {
            Some(row) => Ok(rows_to_dtos(&mut *conn, vec![row]).await?.into_iter().next()),
            None => Ok(None),
        }
    }

    /// `UnlockableTimeSpecialService.findUnlocked(userId)` — the time specials
    /// the user has unlocked (`unlocked_relation` → `object_relations` of type
    /// `TIME_SPECIAL`), each with the requesting user's activation status filled
    /// in. Drives the `time_special_unlocked_change` emit.
    pub async fn find_unlocked_dtos(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<Vec<TimeSpecialDto>> {
        let ids = crate::bo::unlocked_relation_bo::UnlockedRelationBo::find_unlocked_reference_ids(
            &mut *conn,
            user_id,
            crate::model::object_relation::object_enum::TIME_SPECIAL,
        )
        .await?;
        if ids.is_empty() {
            return Ok(Vec::new());
        }
        let placeholders = std::iter::repeat("?")
            .take(ids.len())
            .collect::<Vec<_>>()
            .join(", ");
        let sql = format!("{SELECT_DTO} WHERE ts.id IN ({placeholders}) ORDER BY ts.id");
        let mut query = sqlx::query_as::<_, TimeSpecialRow>(&sql).bind(user_id);
        for id in &ids {
            query = query.bind(*id);
        }
        let rows = query.fetch_all(&mut *conn).await?;
        rows_to_dtos(&mut *conn, rows).await
    }

    /// `ActiveTimeSpecialBo.findByUserWithCurrentStatus(user)` — the
    /// `time_special_change` sync payload. Java (`TimeSpecialRestService`) returns
    /// only the time specials the user has *unlocked*
    /// (`UnlockableTimeSpecialService.findUnlocked`), each with the requesting
    /// user's activation status — i.e. the exact result of `find_unlocked_dtos`.
    /// (`recomputeDates` is reproduced inside the DTO mapping, which computes the
    /// pending millis relative to the current time at query.)
    pub async fn find_user_status_dtos(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<Vec<TimeSpecialDto>> {
        Self::find_unlocked_dtos(&mut *conn, user_id).await
    }
}
