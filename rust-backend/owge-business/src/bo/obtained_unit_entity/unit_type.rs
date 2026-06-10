//! Mirrors the `unit_types` table (cf. `crate::model::unit_type::UnitType`).
//! Only `id`/`name` are read by `find_completed_dtos` (the `unit.typeName`
//! field), but `DeriveEntityModel` needs the whole row mapped.

use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel)]
#[sea_orm(table_name = "unit_types")]
pub struct Model {
    #[sea_orm(primary_key, auto_increment = false)]
    pub id: u16,
    pub name: String,
    pub attack_rule_id: Option<u16>,
    pub max_count: Option<i64>,
    pub share_max_count: Option<u16>,
    pub image_id: Option<u64>,
    pub parent_type: Option<u16>,
    pub can_explore: String,
    pub can_gather: String,
    pub can_establish_base: String,
    pub can_attack: String,
    pub can_counterattack: String,
    pub can_conquest: String,
    pub can_deploy: String,
    pub speed_impact_group_id: Option<u16>,
    pub critical_attack_id: Option<u16>,
    pub has_to_inherit_improvements: i8,
}

#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {}

impl ActiveModelBehavior for ActiveModel {}
