//! Port of the write side of `AttackRuleBo` backing `AdminAttackRuleRestService`
//! (`admin/attack-rule`).
//!
//! Unlike most admin controllers, this one is **not** a `CrudRestServiceTrait`
//! implementation: it exposes only `POST ''` (save new), `PUT '{id}'` (save
//! existing, with the path `id` written onto the body) and `DELETE '{id}'`.
//! There are no `GET` endpoints in the Java controller, so none are ported.
//!
//! `AttackRuleBo.save` is a single transaction that:
//!   1. when the rule already has an id, deletes its existing
//!      `attack_rule_entries` (`deleteByAttackRuleId`),
//!   2. inserts/updates the `attack_rules` row, and
//!   3. re-inserts every entry from the body, each linked to the saved rule.
//! `delete` removes the rule's entries and then the rule itself.
//!
//! The recursion-based `findAttackRule` / `canAttack` combat helpers are part of
//! the mission engine, not this admin endpoint, and are out of scope here.

use crate::db::Db;
use crate::dto::attack_rule::{
    AttackRuleDto, AttackRuleEntryDto, AttackRuleEntryInput, AttackRuleInput,
};
use crate::error::{OwgeError, OwgeResult};
use crate::model::attack_rule::{AttackRule, AttackRuleEntry};

impl From<AttackRuleEntry> for AttackRuleEntryDto {
    fn from(r: AttackRuleEntry) -> Self {
        AttackRuleEntryDto {
            id: r.id,
            target: r.target,
            reference_id: r.reference_id,
            // `@Transient`, never populated on read — always null.
            reference_name: None,
            can_attack: r.can_attack != 0,
        }
    }
}

pub struct AttackRuleBo;

impl AttackRuleBo {
    /// Loads an attack rule with its entries as a DTO, or `None` if absent.
    pub async fn find_by_id(db: &Db, id: u16) -> OwgeResult<Option<AttackRuleDto>> {
        let rule = sqlx::query_as::<_, AttackRule>("SELECT id, name FROM attack_rules WHERE id = ?")
            .bind(id)
            .fetch_optional(db)
            .await?;
        let Some(rule) = rule else {
            return Ok(None);
        };
        let entries = sqlx::query_as::<_, AttackRuleEntry>(
            "SELECT id, attack_rule_id, target, reference_id, can_attack \
             FROM attack_rule_entries WHERE attack_rule_id = ? ORDER BY id",
        )
        .bind(id)
        .fetch_all(db)
        .await?;
        Ok(Some(AttackRuleDto {
            id: rule.id,
            name: rule.name,
            entries: entries.into_iter().map(Into::into).collect(),
        }))
    }

    /// `AttackRuleBo.save` for a brand-new rule — `attack_rules.id` is
    /// AUTO_INCREMENT. Inserts the rule and all of its entries in one
    /// transaction.
    pub async fn save_new(db: &Db, input: &AttackRuleInput) -> OwgeResult<AttackRuleDto> {
        let mut tx = db.begin().await?;
        let result = sqlx::query("INSERT INTO attack_rules (name) VALUES (?)")
            .bind(&input.name)
            .execute(&mut *tx)
            .await?;
        let id = result.last_insert_id() as u16;
        Self::insert_entries(&mut tx, id, &input.entries).await?;
        tx.commit().await?;
        Self::find_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::Common("Attack rule vanished right after insert".into()))
    }

    /// `AttackRuleBo.save` for an existing rule — updates the row, drops the
    /// previous entries (`deleteByAttackRuleId`) and re-inserts the body's
    /// entries, all in one transaction.
    pub async fn save_existing(
        db: &Db,
        id: u16,
        input: &AttackRuleInput,
    ) -> OwgeResult<AttackRuleDto> {
        let mut tx = db.begin().await?;
        let affected = sqlx::query("UPDATE attack_rules SET name = ? WHERE id = ?")
            .bind(&input.name)
            .bind(id)
            .execute(&mut *tx)
            .await?
            .rows_affected();
        if affected == 0 {
            return Err(OwgeError::NotFound(format!("No attack rule with id {id}")));
        }
        sqlx::query("DELETE FROM attack_rule_entries WHERE attack_rule_id = ?")
            .bind(id)
            .execute(&mut *tx)
            .await?;
        Self::insert_entries(&mut tx, id, &input.entries).await?;
        tx.commit().await?;
        Self::find_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No attack rule with id {id}")))
    }

    /// `AttackRuleBo.delete` — removes the rule's entries and then the rule.
    pub async fn delete(db: &Db, id: u16) -> OwgeResult<()> {
        let mut tx = db.begin().await?;
        sqlx::query("DELETE FROM attack_rule_entries WHERE attack_rule_id = ?")
            .bind(id)
            .execute(&mut *tx)
            .await?;
        sqlx::query("DELETE FROM attack_rules WHERE id = ?")
            .bind(id)
            .execute(&mut *tx)
            .await?;
        tx.commit().await?;
        Ok(())
    }

    async fn insert_entries(
        tx: &mut sqlx::Transaction<'_, sqlx::MySql>,
        attack_rule_id: u16,
        entries: &[AttackRuleEntryInput],
    ) -> OwgeResult<()> {
        for entry in entries {
            sqlx::query(
                "INSERT INTO attack_rule_entries \
                    (attack_rule_id, target, reference_id, can_attack) \
                 VALUES (?, ?, ?, ?)",
            )
            .bind(attack_rule_id)
            .bind(&entry.target)
            .bind(entry.reference_id)
            .bind(i8::from(entry.can_attack))
            .execute(&mut **tx)
            .await?;
        }
        Ok(())
    }
}
