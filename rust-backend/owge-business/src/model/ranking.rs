//! Mirrors the legacy `ranking` table (an in-memory MEMORY-engine table from
//! the original PHP game). Note that `RankingBo.findRanking()` does **not** read
//! this table: it builds the ranking live from `user_storage` ordered by points
//! (see [`crate::bo::ranking_bo`]). This entity is kept for schema parity.

use serde::{Deserialize, Serialize};

/// Mirrors the legacy `ranking` table. All numeric columns are `int` (signed).
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct Ranking {
    #[sqlx(rename = "PosicionTotal")]
    pub posicion_total: i32,
    #[sqlx(rename = "usercd")]
    pub usercd: i32,
    #[sqlx(rename = "PuntosTotales")]
    pub puntos_totales: i32,
}
