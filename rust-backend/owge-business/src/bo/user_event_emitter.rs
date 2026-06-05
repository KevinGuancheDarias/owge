//! Port of `UserEventEmitterBo`
//! (`com.kevinguanchedarias.owgejava.business.user.UserEventEmitterBo`).
//!
//! Two emitters:
//! * `emit_user_data`       — `user_data_change` (full player record DTO)
//! * `emit_max_energy_change` — `user_max_energy_change` (a single `f64`)
//!
//! Both follow the canonical wrapper shape: async fn (db, user_id) calling
//! `emitter::send_message` with a lazy async closure, so the DB query only
//! runs when at least one socket is connected for that user.

use crate::db::Db;
use crate::error::OwgeResult;
use crate::websocket::emitter;

pub struct UserEventEmitter;

impl UserEventEmitter {
    /// Emits `user_data_change` — the full `UserStorageDto` for this user.
    ///
    /// Java: `UserEventEmitterBo.emitUserData` -> `socketIoService.sendMessage(user, USER_DATA_CHANGE, () -> findData(user))`.
    /// When `find_data` returns `None` (no user row), the emit is skipped entirely
    /// (mirrors Java's assumption that the user always exists; we guard here just in case).
    pub async fn emit_user_data(db: &Db, user_id: i32) -> OwgeResult<()> {
        emitter::send_message(db, user_id, "user_data_change", || async move {
            let dto = crate::bo::UserStorageBo::find_data(db, user_id).await?;
            // Java always has a user row; skip the emit if somehow missing.
            match dto {
                Some(d) => Ok(serde_json::to_value(d)?),
                None => {
                    // Return a sentinel that the emitter will still push; an absent user
                    // is a programming error, but we should not panic — emit null so the
                    // frontend can handle it gracefully, matching Java's behaviour of
                    // always sending something.
                    Ok(serde_json::Value::Null)
                }
            }
        })
        .await
    }

    /// Emits `user_max_energy_change` — just the max-energy `f64`.
    ///
    /// Java: `UserEventEmitterBo.emitMaxEnergyChange` -> `userEnergyServiceBo.findMaxEnergy(user)`.
    /// We replicate `UserEnergyServiceBo.findMaxEnergy` inline here (it is a
    /// one-liner in Java: `computeImprovementValue(faction.initialEnergy, moreEnergyProduction, true)`).
    /// We do NOT edit `user_storage_bo.rs`; instead we duplicate the two-step DB
    /// read used there (faction.initial_energy + user improvement) and call the
    /// same `compute_improvement_value` helper.
    pub async fn emit_max_energy_change(db: &Db, user_id: i32) -> OwgeResult<()> {
        emitter::send_message(db, user_id, "user_max_energy_change", || async move {
            let max_energy = Self::find_max_energy(db, user_id).await?;
            Ok(serde_json::to_value(max_energy)?)
        })
        .await
    }

    /// Computes the max-energy value for `user_id`.
    ///
    /// Mirrors `UserEnergyServiceBo.findMaxEnergy`:
    /// ```text
    /// computeImprovementValue(faction.initialEnergy, improvement.moreEnergyProduction, true)
    /// ```
    /// The faction row is found via the `user_storage.faction` FK; the improvement
    /// aggregate is obtained from `UserImprovementBo::find_user_improvement`.
    pub async fn find_max_energy(db: &Db, user_id: i32) -> OwgeResult<f64> {
        // 1. Resolve faction.initial_energy for this user.
        let initial_energy: Option<u32> = sqlx::query_scalar(
            "SELECT f.initial_energy \
               FROM factions f \
               JOIN user_storage us ON us.faction = f.id \
              WHERE us.id = ?",
        )
        .bind(user_id)
        .fetch_optional(db)
        .await?;

        // 2. Get the user's improvement aggregate (same call as user_storage_bo::find_data).
        let improvement =
            crate::bo::UserImprovementBo::find_user_improvement(db, user_id).await?;

        // 3. Apply the improvement engine — identical to the call in find_data.
        let max_energy = crate::bo::mission_bo::compute_improvement_value(
            db,
            initial_energy.unwrap_or(0) as f64,
            improvement.more_energy_production,
            true,
        )
        .await?;

        Ok(max_energy)
    }
}
