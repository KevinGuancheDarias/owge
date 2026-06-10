//! Mirrors the `obtained_unit_temporal_information` table (cf.
//! `crate::dto::obtained_unit::TemporalInformationDto`). Leaf in the join
//! graph — no relations needed here.

use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel)]
#[sea_orm(table_name = "obtained_unit_temporal_information")]
pub struct Model {
    #[sea_orm(primary_key, auto_increment = false)]
    pub id: u32,
    pub duration: u32,
    pub expiration: DateTimeUtc,
    pub relation_id: u16,
}

#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {}

impl ActiveModelBehavior for ActiveModel {}
