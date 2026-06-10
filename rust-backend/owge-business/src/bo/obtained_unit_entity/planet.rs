//! Mirrors the `planets` table (cf. `crate::model::planet::Planet`).
//!
//! `obtained_unit::Entity` joins this table twice (as `source_planet` and
//! `target_planet`, aliased `sp`/`tp`); these `Galaxy`/`Owner` `RelationDef`s
//! get rebased onto those aliases via `.from_alias(...)` and re-aliased again
//! (`sp_galaxy`/`sp_owner`/`tp_galaxy`/`tp_owner`) when building the joined
//! query in `obtained_unit_bo.rs`.

use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel)]
#[sea_orm(table_name = "planets")]
pub struct Model {
    #[sea_orm(primary_key, auto_increment = false)]
    pub id: u64,
    pub name: String,
    pub galaxy_id: u16,
    pub sector: u32,
    pub quadrant: u32,
    pub planet_number: u16,
    pub owner: Option<i32>,
    pub richness: u16,
    pub home: Option<i8>,
    pub special_location_id: Option<u16>,
}

#[derive(Copy, Clone, Debug, EnumIter)]
pub enum Relation {
    Galaxy,
    Owner,
}

impl RelationTrait for Relation {
    fn def(&self) -> RelationDef {
        match self {
            Self::Galaxy => Entity::belongs_to(super::galaxy::Entity)
                .from(Column::GalaxyId)
                .to(super::galaxy::Column::Id)
                .into(),
            Self::Owner => Entity::belongs_to(super::user_storage::Entity)
                .from(Column::Owner)
                .to(super::user_storage::Column::Id)
                .into(),
        }
    }
}

impl ActiveModelBehavior for ActiveModel {}
