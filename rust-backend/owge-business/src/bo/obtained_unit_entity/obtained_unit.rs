//! Mirrors the `obtained_units` table (cf. `crate::model::obtained_unit::ObtainedUnit`).

use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, Eq, DeriveEntityModel)]
#[sea_orm(table_name = "obtained_units")]
pub struct Model {
    #[sea_orm(primary_key, auto_increment = false)]
    pub id: u64,
    pub user_id: i32,
    pub unit_id: u16,
    pub count: u64,
    pub source_planet: Option<u64>,
    pub target_planet: Option<u64>,
    pub mission_id: Option<u64>,
    pub first_deployment_mission: Option<u64>,
    pub is_from_capture: i8,
    pub expiration_id: Option<u32>,
    pub owner_unit_id: Option<u64>,
}

/// Two of these (`SourcePlanet`/`TargetPlanet`) target the same `planet::Entity`
/// — SeaORM can't derive both from one `#[derive(DeriveRelation)]` (it can only
/// implement `Related<planet::Entity>` once), so the relations are defined by
/// hand and used directly as `RelationDef`s for `.join_as(...)` in the query
/// builder; no `Related` impls are needed since we don't use
/// `find_also_related`/loaders here.
#[derive(Copy, Clone, Debug, EnumIter)]
pub enum Relation {
    Unit,
    Owner,
    SourcePlanet,
    TargetPlanet,
    TemporalInformation,
}

impl RelationTrait for Relation {
    fn def(&self) -> RelationDef {
        match self {
            Self::Unit => Entity::belongs_to(super::unit::Entity)
                .from(Column::UnitId)
                .to(super::unit::Column::Id)
                .into(),
            Self::Owner => Entity::belongs_to(super::user_storage::Entity)
                .from(Column::UserId)
                .to(super::user_storage::Column::Id)
                .into(),
            Self::SourcePlanet => Entity::belongs_to(super::planet::Entity)
                .from(Column::SourcePlanet)
                .to(super::planet::Column::Id)
                .into(),
            Self::TargetPlanet => Entity::belongs_to(super::planet::Entity)
                .from(Column::TargetPlanet)
                .to(super::planet::Column::Id)
                .into(),
            Self::TemporalInformation => Entity::belongs_to(super::temporal_information::Entity)
                .from(Column::ExpirationId)
                .to(super::temporal_information::Column::Id)
                .into(),
        }
    }
}

impl ActiveModelBehavior for ActiveModel {}
