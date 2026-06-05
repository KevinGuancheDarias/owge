//! Persistence entities for the critical-attack rules, mirroring the Java
//! `CriticalAttack` and `CriticalAttackEntry` JPA entities.
//!
//! Schema (`business/database/02_schema.sql`):
//! - `critical_attack(id smallint UNSIGNED AUTO_INCREMENT, name varchar(100))`
//! - `critical_attack_entries(id int UNSIGNED AUTO_INCREMENT,
//!    critical_attack_id smallint UNSIGNED, target enum('UNIT','UNIT_TYPE'),
//!    reference_id int UNSIGNED, value float)`

/// A `critical_attack` row.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct CriticalAttack {
    pub id: u16,
    pub name: String,
}

/// A `critical_attack_entries` row.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct CriticalAttackEntry {
    pub id: u32,
    pub critical_attack_id: u16,
    /// `enum('UNIT','UNIT_TYPE')`, stored as a string (Java `AttackableTargetEnum`).
    pub target: String,
    pub reference_id: u32,
    pub value: f32,
}
