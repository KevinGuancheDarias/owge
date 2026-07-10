use crate::OwgeResult;
use crate::bo::unit_bo::SELECT_UNIT;
use crate::bo::unit_bo::UnitRow;
use crate::bo::{AttackRuleBo, CriticalAttackBo, ImprovementBo, SpeedImpactGroupBo};
use crate::dto::UnitDto;
use crate::error::OwgeError;
use crate::model::object_relation::object_enum;
use sqlx::MySqlConnection;

pub struct UnlockedUnitFinder {}

impl UnlockedUnitFinder {
    /// `UnitBo.findAllByUser` — the units unlocked for the user, each enriched
    /// with its `improvement`, applicable `speedImpactGroup`, `attackRule` and
    /// `criticalAttack` (Java resolves all four per unit before `toDto`).
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
            let attack_rule_id = row.attack_rule_id;
            let critical_attack_id = row.critical_attack_id;
            let speed_impact_group_id = row.speed_impact_group_id;
            let mut dto: UnitDto = row.into();
            let unit_id = dto.id;
            dto.improvement = Self::resolve_improvement(&mut *conn, unit_id).await?;
            // Java's UnitDto serializes the unit's OWN speedImpactGroup only: a
            // NULL FK drops the key (NON_NULL). The unit-type inheritance
            // fallback (`find_applicable_speed_impact_group`) is gameplay
            // resolution and must not leak into this payload — D19: on level-up
            // completion Java emitted unit 1 (NULL FK) without the key while
            // Rust emitted the type-inherited group.
            dto.speed_impact_group = match speed_impact_group_id {
                Some(group_id) => SpeedImpactGroupBo::find_by_id(&mut *conn, group_id).await?,
                None => None,
            };
            dto.attack_rule = match attack_rule_id {
                Some(id) => AttackRuleBo::find_by_id(&mut *conn, id).await?,
                None => None,
            };
            dto.critical_attack = match critical_attack_id {
                Some(id) => CriticalAttackBo::find_by_id(&mut *conn, id).await?,
                None => None,
            };
            out.push(dto);
        }
        Ok(out)
    }

    /// The unit's own improvement, or `None` when it has no `improvement_id`
    /// (Java's `DtoWithImprovements` leaves the field null, which `NON_NULL`
    /// omits). Uses the *shallow* improvement shape (no nested
    /// `speedImpactGroup.requirementsGroups` on the embedded `unitType` —
    /// see `ImprovementBo::find_for_entity_shallow`'s doc for why).
    async fn resolve_improvement(
        conn: &mut MySqlConnection,
        unit_id: u16,
    ) -> OwgeResult<Option<crate::dto::ImprovementDto>> {
        match ImprovementBo::find_for_entity_shallow(conn, "units", unit_id).await {
            Ok(improvement) => Ok(Some(improvement)),
            Err(OwgeError::NotFound(_)) => Ok(None),
            Err(e) => Err(e),
        }
    }
}
