//! Mirrors the `units` table (cf. `crate::model::unit::Unit` /
//! `crate::bo::unit_bo::UnitRow`).

use crate::model::seaorm::image_store;
use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, DeriveEntityModel)]
#[sea_orm(table_name = "units")]
pub struct Model {
    #[sea_orm(primary_key, auto_increment = false)]
    pub id: u16,
    pub order_number: Option<u16>,
    pub name: String,
    pub display_in_requirements: Option<i8>,
    pub attack_rule_id: Option<u16>,
    pub image_id: Option<u64>,
    pub points: Option<u32>,
    pub description: Option<String>,
    pub time: Option<i32>,
    pub primary_resource: Option<u32>,
    pub secondary_resource: Option<u32>,
    pub energy: Option<u16>,
    #[sea_orm(column_name = "type")]
    pub type_id: Option<u16>,
    pub attack: Option<u16>,
    pub health: Option<u16>,
    pub shield: Option<u16>,
    pub charge: Option<u16>,
    pub is_unique: u8,
    pub can_fast_explore: i8,
    pub speed: Option<f64>,
    pub improvement_id: u16,
    pub cloned_improvements: i8,
    pub speed_impact_group_id: Option<u16>,
    pub critical_attack_id: Option<u16>,
    pub bypass_shield: i8,
    pub is_invisible: i8,
    pub stored_weight: u32,
    pub storage_capacity: Option<u32>,
}

#[derive(Copy, Clone, Debug, EnumIter)]
pub enum Relation {
    UnitType,
    ImageStore,
}

impl RelationTrait for Relation {
    fn def(&self) -> RelationDef {
        match self {
            Self::UnitType => Entity::belongs_to(super::unit_type::Entity)
                .from(Column::TypeId)
                .to(super::unit_type::Column::Id)
                .into(),
            Self::ImageStore => Entity::belongs_to(image_store::Entity)
                .from(Column::ImageId)
                .to(image_store::Column::Id)
                .into(),
        }
    }
}

impl ActiveModelBehavior for ActiveModel {}
