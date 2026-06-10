//! Port of (the read side of) `UnlockedRelationBo` — the unlock engine.
//!
//! A row in `unlocked_relation` means "this user may use this `object_relation`".
//! `find_unlocked_reference_ids` resolves, for a user and an `ObjectEnum` type,
//! the concrete entity ids the user has unlocked (via the `object_relations`
//! indirection) — the basis for `UnitBo.findAllByUser`, the unlocked
//! speed-impact-groups, the available time-specials, etc.

use crate::error::OwgeResult;
use sqlx::MySqlConnection;

pub struct UnlockedRelationBo;

impl UnlockedRelationBo {
    /// `findByUserIdAndObjectType(userId, type)` unboxed to the referenced
    /// entity ids. `reference_id` is `smallint` (signed) in the schema.
    pub async fn find_unlocked_reference_ids(
        conn: &mut MySqlConnection,
        user_id: i32,
        object_description: &str,
    ) -> OwgeResult<Vec<i16>> {
        let ids = sqlx::query_scalar::<_, i16>(
            "SELECT o.reference_id \
             FROM unlocked_relation ur \
             JOIN object_relations o ON o.id = ur.relation_id \
             WHERE ur.user_id = ? AND o.object_description = ?",
        )
        .bind(user_id)
        .bind(object_description)
        .fetch_all(&mut *conn)
        .await?;
        Ok(ids)
    }
}
