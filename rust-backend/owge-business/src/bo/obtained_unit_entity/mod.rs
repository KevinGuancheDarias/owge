//! SeaORM entities scoped to `ObtainedUnitBo::find_completed_dtos`'s single
//! joined query (`obtained_unit_bo.rs`).
//!
//! This is a deliberate, narrowly-scoped exception to the crate's "no ORM,
//! hand-written `sqlx` queries" convention (see `db.rs`): that query joins
//! `planets`/`galaxies`/`user_storage` *twice* (source/target), a shape
//! `sqlx::FromRow` can't decode into nested structs without column collisions.
//! SeaORM's `DerivePartialModel` (`#[sea_orm(nested, alias = "...")]`) handles
//! that aliasing automatically. These entities intentionally mirror (a subset
//! of) the same tables as `crate::model::*` — kept separate so the fork stays
//! local to this one query.

pub mod galaxy;
pub mod obtained_unit;
pub mod planet;
pub mod temporal_information;
pub mod unit;
pub mod unit_type;
pub mod user_storage;
