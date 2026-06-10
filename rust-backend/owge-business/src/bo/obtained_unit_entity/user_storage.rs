//! Mirrors the `user_storage` table (cf. `crate::model::user_storage::UserStorage`).
//! Leaf in the join graph for our purposes — only `username` is read out of
//! the joined "owner" rows, but `DeriveEntityModel` needs the whole row mapped.
//! Not `Eq` — has `f64` columns.

use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, DeriveEntityModel)]
#[sea_orm(table_name = "user_storage")]
pub struct Model {
    #[sea_orm(primary_key, auto_increment = false)]
    pub id: i32,
    pub username: String,
    pub email: String,
    pub alliance_id: Option<u16>,
    pub faction: u16,
    pub last_action: DateTime,
    pub home_planet: u64,
    pub primary_resource: Option<f64>,
    pub secondary_resource: Option<f64>,
    pub energy: f64,
    pub primary_resource_generation_per_second: Option<f64>,
    pub secondary_resource_generation_per_second: Option<f64>,
    pub has_skipped_tutorial: bool,
    pub points: f64,
    pub can_alter_twitch_state: bool,
    pub banned: bool,
}

#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {}

impl ActiveModelBehavior for ActiveModel {}
