//! Persistence entities for the attack-rule system, the Rust counterpart of the
//! Java `AttackRule` / `AttackRuleEntry` `@Entity` classes.
//!
//! `attack_rules(id smallint unsigned AUTO_INCREMENT, name varchar(100))` and
//! `attack_rule_entries(id smallint unsigned AUTO_INCREMENT, attack_rule_id
//! smallint unsigned, target enum('UNIT','UNIT_TYPE'), reference_id smallint
//! unsigned, can_attack tinyint)`.

use sqlx::FromRow;

/// A row of `attack_rules`.
#[derive(Debug, Clone, FromRow)]
pub struct AttackRule {
    pub id: u16,
    pub name: String,
}

/// A row of `attack_rule_entries`. `can_attack` is `tinyint` so it maps to `i8`
/// at the DB boundary and is exposed as a bool in the DTO.
#[derive(Debug, Clone, FromRow)]
pub struct AttackRuleEntry {
    pub id: u16,
    pub attack_rule_id: u16,
    pub target: String,
    pub reference_id: u16,
    pub can_attack: i8,
}
