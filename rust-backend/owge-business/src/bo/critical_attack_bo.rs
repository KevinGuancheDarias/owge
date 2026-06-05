//! Port of the admin write side of `CriticalAttackBo`.
//!
//! Backs `AdminCriticalAttackRestService` (`admin/critical-attack`): the
//! controller exposes only `POST ''`, `PUT '{id}'` and `DELETE '{id}'`. Each
//! save replaces the rule's entries wholesale (the Java code deletes the
//! existing `critical_attack_entries` before re-inserting the body's entries),
//! and clamps negative entry values to their absolute value.
//!
//! The combat-time read helpers (`findApplicableCriticalEntry`,
//! `findUsedCriticalAttack`, `buildFullInformation`) are part of the mission
//! system and are out of scope for this admin port.

use crate::db::Db;
use crate::dto::critical_attack_information::CriticalAttackInformationResponse;
use crate::dto::{CriticalAttackDto, CriticalAttackEntryDto, CriticalAttackInput};
use crate::error::{OwgeError, OwgeResult};
use crate::model::critical_attack::{CriticalAttack, CriticalAttackEntry};

/// `AttackableTargetEnum.UNIT` / `UNIT_TYPE`, stored as text.
const TARGET_UNIT: &str = "UNIT";
const DEFAULT_VALUE_WHEN_MISSING: f32 = 1.0;

pub struct CriticalAttackBo;

impl CriticalAttackBo {
    /// Loads a `critical_attack` and its entries as a [`CriticalAttackDto`]
    /// (mirrors `CriticalAttackBo.toDto`). Returns `None` if the row is missing.
    pub async fn find_by_id(db: &Db, id: u16) -> OwgeResult<Option<CriticalAttackDto>> {
        let row = sqlx::query_as::<_, CriticalAttack>(
            "SELECT id, name FROM critical_attack WHERE id = ?",
        )
        .bind(id)
        .fetch_optional(db)
        .await?;
        let Some(row) = row else {
            return Ok(None);
        };
        let entries = sqlx::query_as::<_, CriticalAttackEntry>(
            "SELECT id, critical_attack_id, target, reference_id, value \
             FROM critical_attack_entries WHERE critical_attack_id = ? ORDER BY id",
        )
        .bind(id)
        .fetch_all(db)
        .await?;
        Ok(Some(CriticalAttackDto {
            id: row.id,
            name: row.name,
            entries: entries
                .into_iter()
                .map(|e| CriticalAttackEntryDto {
                    id: e.id,
                    target: e.target,
                    reference_id: e.reference_id,
                    reference_name: None,
                    value: e.value,
                })
                .collect(),
        }))
    }

    /// `CriticalAttackBo.buildFullInformation(criticalAttack)` — the per-unit
    /// `GET game/unit/{unitId}/criticalAttack` payload for a given critical
    /// attack id.
    ///
    /// One response per `UNIT`-targeted entry (resolving the unit's name), plus
    /// one per unit type (each gets its first matching rule's value, walking the
    /// unit type's parent chain, or the default `1.0`). Sorted by descending
    /// value the same way Java does (`b.value*1000 - a.value*1000`).
    pub async fn build_full_information(
        db: &Db,
        critical_attack_id: u16,
    ) -> OwgeResult<Vec<CriticalAttackInformationResponse>> {
        let entries = sqlx::query_as::<_, CriticalAttackEntry>(
            "SELECT id, critical_attack_id, target, reference_id, value \
             FROM critical_attack_entries WHERE critical_attack_id = ? ORDER BY id",
        )
        .bind(critical_attack_id)
        .fetch_all(db)
        .await?;

        // unit_types.findAll() ordered by id (mirrors UnitTypeBo.findAll), as
        // (id, name, parent_type) — parent chain used by findUnitTypeMatchingRule.
        let unit_types = sqlx::query_as::<_, (u16, String, Option<u16>)>(
            "SELECT id, name, parent_type FROM unit_types ORDER BY id",
        )
        .fetch_all(db)
        .await?;
        let parent_of: std::collections::HashMap<u16, Option<u16>> =
            unit_types.iter().map(|(id, _, p)| (*id, *p)).collect();

        let mut ret_val: Vec<CriticalAttackInformationResponse> = Vec::new();

        // Entries that target a concrete unit.
        for entry in entries.iter().filter(|e| e.target == TARGET_UNIT) {
            ret_val.push(Self::map_entry_to_information_response(db, entry).await?);
        }

        // One per unit type: its first matching rule, or the default value.
        for (type_id, type_name, _) in &unit_types {
            let matching = entries
                .iter()
                .find(|entry| Self::matches_unit_type(entry.reference_id, *type_id, &parent_of));
            match matching {
                Some(entry) => {
                    ret_val.push(Self::map_entry_to_information_response(db, entry).await?);
                }
                None => ret_val.push(CriticalAttackInformationResponse {
                    target: "UNIT_TYPE".to_string(),
                    target_id: *type_id as u32,
                    target_name: type_name.clone(),
                    value: DEFAULT_VALUE_WHEN_MISSING,
                }),
            }
        }

        // b.value*1000 - a.value*1000 (descending value).
        ret_val.sort_by(|a, b| {
            ((b.value * 1000.0) as i64).cmp(&((a.value * 1000.0) as i64))
        });
        Ok(ret_val)
    }

    /// `CriticalAttackBo.findUnitTypeMatchingRule` — true when `reference_id`
    /// equals `type_id` or any of its ancestors (walking `parent_type`).
    fn matches_unit_type(
        reference_id: u32,
        type_id: u16,
        parent_of: &std::collections::HashMap<u16, Option<u16>>,
    ) -> bool {
        let mut current = Some(type_id);
        while let Some(tid) = current {
            if reference_id == tid as u32 {
                return true;
            }
            current = parent_of.get(&tid).copied().flatten();
        }
        false
    }

    /// `CriticalAttackBo.mapEntryToInformationResponse` — resolves the target's
    /// name (`unit` name when target is `UNIT`, else the `unit_type` name).
    async fn map_entry_to_information_response(
        db: &Db,
        entry: &CriticalAttackEntry,
    ) -> OwgeResult<CriticalAttackInformationResponse> {
        let target_name: String = if entry.target == TARGET_UNIT {
            sqlx::query_scalar("SELECT name FROM units WHERE id = ?")
                .bind(entry.reference_id)
                .fetch_optional(db)
                .await?
                .ok_or_else(|| {
                    OwgeError::NotFound(format!("No unit with id {}", entry.reference_id))
                })?
        } else {
            sqlx::query_scalar("SELECT name FROM unit_types WHERE id = ?")
                .bind(entry.reference_id)
                .fetch_optional(db)
                .await?
                .ok_or_else(|| {
                    OwgeError::NotFound(format!("No unit type with id {}", entry.reference_id))
                })?
        };
        Ok(CriticalAttackInformationResponse {
            target: entry.target.clone(),
            target_id: entry.reference_id,
            target_name,
            value: entry.value,
        })
    }

    /// `CriticalAttackBo.save` for a create (no id) — inserts the rule then its
    /// entries. `critical_attack.id` is AUTO_INCREMENT.
    pub async fn save_new(db: &Db, input: &CriticalAttackInput) -> OwgeResult<CriticalAttackDto> {
        let result = sqlx::query("INSERT INTO critical_attack (name) VALUES (?)")
            .bind(&input.name)
            .execute(db)
            .await?;
        let id = result.last_insert_id() as u16;
        Self::insert_entries(db, id, input).await?;
        Self::find_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::Common("Critical attack vanished right after insert".into()))
    }

    /// `CriticalAttackBo.save` for an update — replaces the row's name and all
    /// of its entries (delete-then-insert, matching the Java code).
    pub async fn save_existing(
        db: &Db,
        id: u16,
        input: &CriticalAttackInput,
    ) -> OwgeResult<CriticalAttackDto> {
        let affected = sqlx::query("UPDATE critical_attack SET name = ? WHERE id = ?")
            .bind(&input.name)
            .bind(id)
            .execute(db)
            .await?
            .rows_affected();
        if affected == 0 {
            return Err(OwgeError::NotFound(format!("No critical attack with id {id}")));
        }
        sqlx::query("DELETE FROM critical_attack_entries WHERE critical_attack_id = ?")
            .bind(id)
            .execute(db)
            .await?;
        Self::insert_entries(db, id, input).await?;
        Self::find_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No critical attack with id {id}")))
    }

    /// Inserts the body's entries for the given critical attack, clamping
    /// negative values to their absolute value (Java `Math.abs`).
    async fn insert_entries(db: &Db, id: u16, input: &CriticalAttackInput) -> OwgeResult<()> {
        for entry in &input.entries {
            let value = entry.value.abs();
            sqlx::query(
                "INSERT INTO critical_attack_entries \
                    (critical_attack_id, target, reference_id, value) VALUES (?, ?, ?, ?)",
            )
            .bind(id)
            .bind(&entry.target)
            .bind(entry.reference_id)
            .bind(value)
            .execute(db)
            .await?;
        }
        Ok(())
    }

    /// `CriticalAttackBo.delete(Integer)` — removes the rule's entries then the
    /// rule itself.
    pub async fn delete(db: &Db, id: u16) -> OwgeResult<()> {
        sqlx::query("DELETE FROM critical_attack_entries WHERE critical_attack_id = ?")
            .bind(id)
            .execute(db)
            .await?;
        sqlx::query("DELETE FROM critical_attack WHERE id = ?")
            .bind(id)
            .execute(db)
            .await?;
        Ok(())
    }
}
