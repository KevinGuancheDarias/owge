//! Port of `ConfigurationBo` — reads/writes the `configuration` key/value table
//! that holds engine settings (including the JWT secrets).

use crate::db::Db;
use crate::error::{OwgeError, OwgeResult};
use crate::model::Configuration;

pub struct ConfigurationBo;

impl ConfigurationBo {
    /// `ConfigurationBo.findConfigurationParam` — returns the row or `NotFound`.
    pub async fn find(db: &Db, name: &str) -> OwgeResult<Configuration> {
        Self::find_opt(db, name)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No configuration with name {name}")))
    }

    pub async fn find_opt(db: &Db, name: &str) -> OwgeResult<Option<Configuration>> {
        let row = sqlx::query_as::<_, Configuration>(
            "SELECT name, display_name, value, privileged FROM configuration WHERE name = ?",
        )
        .bind(name)
        .fetch_optional(db)
        .await?;
        Ok(row)
    }

    /// `ConfigurationBo.findOrSetDefault` — read, or insert a default and
    /// return it. Used at boot to materialize JWT secrets/algos/durations.
    pub async fn find_or_set_default(
        db: &Db,
        name: &str,
        default_value: &str,
    ) -> OwgeResult<Configuration> {
        if let Some(existing) = Self::find_opt(db, name).await? {
            return Ok(existing);
        }
        sqlx::query("INSERT INTO configuration (name, value, privileged) VALUES (?, ?, 0)")
            .bind(name)
            .bind(default_value)
            .execute(db)
            .await?;
        Ok(Configuration {
            name: name.to_string(),
            display_name: None,
            value: default_value.to_string(),
            privileged: 0,
        })
    }

    /// Convenience: just the value string for a code.
    pub async fn find_value(db: &Db, name: &str) -> OwgeResult<String> {
        Ok(Self::find(db, name).await?.value)
    }

    /// `ConfigurationBo.findConfiguration` for the open endpoint — only the
    /// non-privileged rows are exposed publicly.
    pub async fn find_public(db: &Db) -> OwgeResult<Vec<Configuration>> {
        let rows = sqlx::query_as::<_, Configuration>(
            "SELECT name, display_name, value, privileged FROM configuration WHERE privileged = 0",
        )
        .fetch_all(db)
        .await?;
        Ok(rows)
    }

    /// `ConfigurationBo.findAllNonPrivileged` — every non-privileged row, used
    /// by the admin `GET admin/configuration` listing. (Same predicate as
    /// [`find_public`](Self::find_public); kept separate to mirror the Java
    /// method names.)
    pub async fn find_all_non_privileged(db: &Db) -> OwgeResult<Vec<Configuration>> {
        Self::find_public(db).await
    }

    /// `ConfigurationBo.save` — upsert a row by `name`. Re-implements the
    /// `MISSION_TIME_*` guard (value must parse to `>= 10`, else
    /// `SgtBackendInvalidInputException`). `privileged` is preserved on update
    /// and defaults to `0` on insert, matching the Java entity default.
    pub async fn save(
        db: &Db,
        name: &str,
        display_name: Option<&str>,
        value: &str,
    ) -> OwgeResult<Configuration> {
        if Self::is_of_type_mission_time(name) {
            Self::check_can_save_mission_time(name, value)?;
        }
        // ON DUPLICATE KEY UPDATE keeps the existing `privileged` flag (only
        // display_name/value are updated), exactly like saving the JPA entity
        // built from the DTO would (privileged is not part of the DTO).
        sqlx::query(
            "INSERT INTO configuration (name, display_name, value, privileged) VALUES (?, ?, ?, 0) \
             ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), value = VALUES(value)",
        )
        .bind(name)
        .bind(display_name)
        .bind(value)
        .execute(db)
        .await?;
        Self::find(db, name).await
    }

    /// `ConfigurationBo.deleteOne`.
    pub async fn delete_one(db: &Db, name: &str) -> OwgeResult<()> {
        sqlx::query("DELETE FROM configuration WHERE name = ?")
            .bind(name)
            .execute(db)
            .await?;
        Ok(())
    }

    /// `ConfigurationBo.isOfTypeMissionTime` — name begins with `MISSION_TIME_`.
    fn is_of_type_mission_time(name: &str) -> bool {
        name.starts_with(MISSION_TIME_INDEX_OF_KEY)
    }

    /// `ConfigurationBo.checkCanSaveMisisonTyme` — the value must parse to a
    /// number `>= 10` (a non-numeric value parses to `0` and is rejected).
    fn check_can_save_mission_time(name: &str, value: &str) -> OwgeResult<()> {
        let parsed: i64 = value.trim().parse().unwrap_or(0);
        if parsed < MISSION_TIME_MINIMUM_VALUE {
            return Err(OwgeError::InvalidInput(format!(
                "Invalid value {value} for param {name} the value must be {MISSION_TIME_MINIMUM_VALUE} or grater"
            )));
        }
        Ok(())
    }
}

const MISSION_TIME_INDEX_OF_KEY: &str = "MISSION_TIME_";
const MISSION_TIME_MINIMUM_VALUE: i64 = 10;
