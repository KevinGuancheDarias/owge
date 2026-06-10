//! Mirrors the `galaxies` table (cf. `crate::model::galaxy::Galaxy`). Leaf in
//! the join graph from `planet`'s perspective — no relations needed here.

use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel)]
#[sea_orm(table_name = "galaxies")]
pub struct Model {
    #[sea_orm(primary_key, auto_increment = false)]
    pub id: u16,
    pub name: String,
    pub sectors: u32,
    pub quadrants: u32,
    pub num_planets: u32,
    pub order_number: Option<u16>,
}

#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {}

impl ActiveModelBehavior for ActiveModel {}
