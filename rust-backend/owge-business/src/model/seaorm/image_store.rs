use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, DeriveEntityModel)]
#[sea_orm(table_name = "images_store")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: u64,
    pub checksum: String,
    pub filename: String,
}

#[derive(Clone, Debug, PartialEq, DerivePartialModel)]
#[sea_orm(entity = "Entity", from_query_result)]
pub struct ImageSimple {
    pub id: u64,
    pub filename: String,
}

#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {}

impl ActiveModelBehavior for ActiveModel {}
