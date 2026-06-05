//! Port of (the read side of) the time-special domain business objects:
//! `com.kevinguanchedarias.owgejava.business.TimeSpecialBo` and
//! `...business.ActiveTimeSpecialBo`.
//!
//! Direct reads back `GET game/time_special` (list) and the by-id read,
//! building `TimeSpecialDto`s with the image URL resolved via a join and the
//! per-user activation status (`activeTimeSpecialDto`) filled from
//! `active_time_specials`, matching `TimeSpecialBo.toDto`.
//!
//! The `time_special_change` sync payload
//! (`ActiveTimeSpecialBo.findByUserWithCurrentStatus`) only returns the time
//! specials the user has *unlocked*, which depends on the requirement/unlock
//! system, so it is stubbed to an empty list here — see
//! [`TimeSpecialBo::find_user_status_dtos`].

use crate::db::Db;
use crate::dto::time_special::{ActiveTimeSpecialDto, TimeSpecialDto};
use crate::error::OwgeResult;

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

    // --- the user's active_time_specials row (all NULL when not active) ---
    active_id: Option<u64>,
    active_state: Option<String>,
    active_activation_date: Option<chrono::NaiveDateTime>,
    active_expiring_date: Option<chrono::NaiveDateTime>,
    active_ready_date: Option<chrono::NaiveDateTime>,
}

impl From<TimeSpecialRow> for TimeSpecialDto {
    fn from(r: TimeSpecialRow) -> Self {
        let image_url = r.image_filename.map(|f| crate::bo::image_store_bo::compute_image_url(&f));
        let active_time_special_dto = match (r.active_id, r.active_state) {
            (Some(id), Some(state)) => {
                let activation_date =
                    r.active_activation_date.map(millis).unwrap_or_default();
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

const SELECT_DTO: &str = "\
    SELECT ts.id, ts.name, ts.description, \
           ts.image_id AS image, i.filename AS image_filename, \
           ts.duration, ts.recharge_time AS recharge_time, \
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
    pub async fn find_all_dtos(db: &Db, user_id: i32) -> OwgeResult<Vec<TimeSpecialDto>> {
        let rows = sqlx::query_as::<_, TimeSpecialRow>(&format!("{SELECT_DTO} ORDER BY ts.id"))
            .bind(user_id)
            .fetch_all(db)
            .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `GET game/time_special/{id}` — a single time special with the requesting
    /// user's activation status, or `None` when it does not exist.
    pub async fn find_dto_by_id(
        db: &Db,
        user_id: i32,
        id: u16,
    ) -> OwgeResult<Option<TimeSpecialDto>> {
        let row = sqlx::query_as::<_, TimeSpecialRow>(&format!("{SELECT_DTO} WHERE ts.id = ?"))
            .bind(user_id)
            .bind(id)
            .fetch_optional(db)
            .await?;
        Ok(row.map(Into::into))
    }

    /// `UnlockableTimeSpecialService.findUnlocked(userId)` — the time specials
    /// the user has unlocked (`unlocked_relation` → `object_relations` of type
    /// `TIME_SPECIAL`), each with the requesting user's activation status filled
    /// in. Drives the `time_special_unlocked_change` emit.
    pub async fn find_unlocked_dtos(db: &Db, user_id: i32) -> OwgeResult<Vec<TimeSpecialDto>> {
        let ids = crate::bo::unlocked_relation_bo::UnlockedRelationBo::find_unlocked_reference_ids(
            db,
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
        let rows = query.fetch_all(db).await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `ActiveTimeSpecialBo.findByUserWithCurrentStatus(user)` — the
    /// `time_special_change` sync payload. Java (`TimeSpecialRestService`) returns
    /// only the time specials the user has *unlocked*
    /// (`UnlockableTimeSpecialService.findUnlocked`), each with the requesting
    /// user's activation status — i.e. the exact result of `find_unlocked_dtos`.
    /// (`recomputeDates` is reproduced inside the DTO mapping, which computes the
    /// pending millis relative to the current time at query.)
    pub async fn find_user_status_dtos(
        db: &Db,
        user_id: i32,
    ) -> OwgeResult<Vec<TimeSpecialDto>> {
        Self::find_unlocked_dtos(db, user_id).await
    }
}
