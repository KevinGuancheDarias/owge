use crate::OwgeResult;
use crate::bo::unit_bo::SELECT_UNIT;
use crate::bo::unit_bo::UnitRow;
use crate::bo::{ImprovementBo, SpeedImpactGroupBo, UnitInterceptionFinderBo};
use crate::dto::UnitDto;
use crate::error::OwgeError;
use crate::model::object_relation::object_enum;
use sqlx::MySqlConnection;

pub struct UnlockedUnitFinder {}

impl UnlockedUnitFinder {
    /// `UnitBo.findAllByUser` — the units unlocked for the user, each enriched
    /// with its `improvement` and applicable `speedImpactGroup` (Java resolves
    /// both per unit before `toDto`).
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

        let mut out = Vec::with_capacity(rows.len());
        for row in rows {
            let mut dto: UnitDto = row.into();
            let unit_id = dto.id;
            dto.improvement = Self::resolve_improvement(&mut *conn, unit_id).await?;
            dto.speed_impact_group = match UnitInterceptionFinderBo::find_applicable_speed_impact_group(
                &mut *conn, user_id, unit_id,
            )
            .await?
            {
                Some(group_id) => SpeedImpactGroupBo::find_by_id(&mut *conn, group_id).await?,
                None => None,
            };
            out.push(dto);
        }
        Ok(out)
    }

    /// The unit's own improvement, or `None` when it has no `improvement_id`
    /// (Java's `DtoWithImprovements` leaves the field null, which `NON_NULL` omits).
    async fn resolve_improvement(
        conn: &mut MySqlConnection,
        unit_id: u16,
    ) -> OwgeResult<Option<crate::dto::ImprovementDto>> {
        match ImprovementBo::find_for_entity(conn, "units", unit_id).await {
            Ok(improvement) => Ok(Some(improvement)),
            Err(OwgeError::NotFound(_)) => Ok(None),
            Err(e) => Err(e),
        }
    }
}
