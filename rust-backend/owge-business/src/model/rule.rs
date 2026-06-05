//! Persistence entity for the rule system, the Rust counterpart of the Java
//! `Rule` `@Entity` class (`@Table(name = "rules")`).
//!
//! `rules(id smallint unsigned AUTO_INCREMENT, type varchar(50), origin_type
//! varchar(50), origin_id smallint, destination_type varchar(50),
//! destination_id smallint, extra_args varchar(100) NULL)`.
//!
//! `origin_id` / `destination_id` are signed `smallint` columns, so they map to
//! `i16` at the DB boundary even though the Java DTO exposes them as `Long`.
//! `extra_args` is a `#`-delimited string (see [`crate::bo::rule_bo::ARGS_DELIMITER`]).

use sqlx::FromRow;

/// A row of `rules`.
#[derive(Debug, Clone, FromRow)]
pub struct Rule {
    pub id: u16,
    pub r#type: String,
    pub origin_type: String,
    pub origin_id: i16,
    pub destination_type: String,
    pub destination_id: i16,
    pub extra_args: Option<String>,
}
