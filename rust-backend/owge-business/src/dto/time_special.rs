//! DTOs for the time-special domain, mirroring
//! `com.kevinguanchedarias.owgejava.dto.TimeSpecialDto` and
//! `...dto.ActiveTimeSpecialDto`.
//!
//! Field names are Jackson camelCase. The backend serializes `java.util.Date`
//! with `WRITE_DATES_AS_TIMESTAMPS=true`, so every date field below is an epoch
//! millis number (`i64`), matching the JSON the frontend already consumes.
//!
//! As with the other ported catalog DTOs, the nested objects driven by other
//! domains — `improvement` (improvement engine, M2) and `requirements`
//! (requirement system, M3) — are deferred and not emitted yet.

use serde::Serialize;

/// JSON payload for one time special, mirroring `TimeSpecialDto` (which extends
/// `CommonDtoWithImageStore` / `CommonDto`).
///
/// `improvement` and `requirements` are omitted until the improvement engine
/// (M2) and requirement system (M3) are ported. `activeTimeSpecialDto` is the
/// per-user activation status, populated when the special is currently
/// active/recharging for the requesting user.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TimeSpecialDto {
    pub id: u16,
    pub name: String,
    pub description: Option<String>,
    /// The image id (`CommonDtoWithImageStore.image`), or null when no image.
    pub image: Option<u64>,
    /// The resolved image URL (`CommonDtoWithImageStore.imageUrl`). Built from
    /// the image filename in the `Bo` query.
    pub image_url: Option<String>,
    /// `bigint unsigned` — duration in seconds.
    pub duration: u64,
    /// `bigint unsigned` — recharge time in seconds.
    pub recharge_time: u64,
    /// The per-user activation status, or null when the special is not active
    /// nor recharging for the user (`TimeSpecialDto.activeTimeSpecialDto`).
    pub active_time_special_dto: Option<ActiveTimeSpecialDto>,
}

/// JSON payload for one active time special, mirroring `ActiveTimeSpecialDto`.
/// Date fields are epoch millis numbers (see module docs).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ActiveTimeSpecialDto {
    pub id: u64,
    /// The owning time special id (`ActiveTimeSpecialDto.timeSpecial`).
    pub time_special: u16,
    /// `"ACTIVE"` or `"RECHARGE"`.
    pub state: String,
    /// Epoch millis (`activationDate`).
    pub activation_date: i64,
    /// Epoch millis (`expiringDate`).
    pub expiring_date: i64,
    /// Epoch millis (`readyDate`), or null while the special is still active.
    /// Omitted when null, matching Java's global `NON_NULL` inclusion.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ready_date: Option<i64>,
    /// Millis remaining until the effect ends (ACTIVE) or the special becomes
    /// ready again (RECHARGE) — `ActiveTimeSpecialDto.calculatePendingMillis`,
    /// computed at read time.
    pub pending_millis: i64,
}
