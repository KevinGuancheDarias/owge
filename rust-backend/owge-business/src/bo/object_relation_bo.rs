//! Port of the parts of `ObjectRelationBo` needed by the admin write side:
//! `findOne`, `create`, and `findObjectRelationOrCreate` over the
//! `object_relations` table. Every method takes a `&mut MySqlConnection` so it
//! can participate in the caller's transaction (the requirement/group writes run
//! multiple statements that must be atomic).

use sqlx::MySqlConnection;

use crate::error::OwgeResult;

pub struct ObjectRelationBo;

impl ObjectRelationBo {
    /// `objectRelationsRepository.findOneByObjectCodeAndReferenceId` — the
    /// relation id for `(object_description, reference_id)`, or `None`.
    pub async fn find_one(
        conn: &mut MySqlConnection,
        object_description: &str,
        reference_id: i16,
    ) -> OwgeResult<Option<u16>> {
        let id = sqlx::query_scalar::<_, u16>(
            "SELECT id FROM object_relations \
             WHERE object_description = ? AND reference_id = ? LIMIT 1",
        )
        .bind(object_description)
        .bind(reference_id)
        .fetch_optional(&mut *conn)
        .await?;
        Ok(id)
    }

    /// `ObjectRelationBo.create` — insert a new `object_relations` row and return
    /// its AUTO_INCREMENT id.
    pub async fn create(
        conn: &mut MySqlConnection,
        object_description: &str,
        reference_id: i16,
    ) -> OwgeResult<u16> {
        let result = sqlx::query(
            "INSERT INTO object_relations (object_description, reference_id) VALUES (?, ?)",
        )
        .bind(object_description)
        .bind(reference_id)
        .execute(&mut *conn)
        .await?;
        Ok(result.last_insert_id() as u16)
    }

    /// `ObjectRelationBo.findObjectRelationOrCreate` — find-or-insert the
    /// relation for `(object_description, reference_id)`.
    pub async fn find_object_relation_or_create(
        conn: &mut MySqlConnection,
        object_description: &str,
        reference_id: i16,
    ) -> OwgeResult<u16> {
        if let Some(id) = Self::find_one(conn, object_description, reference_id).await? {
            Ok(id)
        } else {
            Self::create(conn, object_description, reference_id).await
        }
    }
}
