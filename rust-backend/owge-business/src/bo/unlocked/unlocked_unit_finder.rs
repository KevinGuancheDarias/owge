use crate::OwgeResult;
use crate::bo::unit_bo::SELECT_UNIT;
use crate::bo::unit_bo::UnitRow;
use crate::dto::UnitDto;
use crate::model::object_relation::object_enum;
use sqlx::MySqlConnection;

pub struct UnlockedUnitFinder {}

impl UnlockedUnitFinder {
    /// `UnitBo.findAllByUser` — the units unlocked for the user.
    pub async fn find_unlocked_by_user(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<Vec<UnitDto>> {
        let rows = sqlx::query_as::<_, UnitRow>(&format!(
            "{SELECT_UNIT} \
             JOIN object_relations o ON o.object_description = ? AND o.reference_id = u.id \
             JOIN unlocked_relation ur ON ur.relation_id = o.id AND ur.user_id = ? \
             ORDER BY u.order_number IS NULL, u.order_number, u.id"
        ))
        .bind(object_enum::UNIT)
        .bind(user_id)
        .fetch_all(&mut *conn)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }
}
