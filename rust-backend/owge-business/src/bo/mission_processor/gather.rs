//! Port of `GatherMissionProcessor`
//! (`...business.mission.processor.GatherMissionProcessor`).
//!
//! Optionally triggers a defensive attack at the target; if the mission survives,
//! computes the gathered resources (sum of `charge * count` over the involved
//! units, scaled by the planet's rational richness and the user's
//! `moreChargeCapacity` improvement, then split into primary/secondary by the
//! faction's custom percentages or 50/50), credits the user, and builds the
//! gather report.

use sqlx::MySqlConnection;

use crate::bo::return_mission_registration_bo::ReturnMissionRegistrationBo;
use crate::bo::user_improvement_bo::UserImprovementBo;
use crate::builder::UnitMissionReportBuilder;
use crate::db::Db;
use crate::error::OwgeResult;
use crate::model::mission::{Mission, MissionType};
use crate::model::obtained_unit::ObtainedUnit;

use super::{attack, create_report_base};

pub async fn process(
    conn: &mut MySqlConnection,
    mission: &Mission,
    involved_units: &[ObtainedUnit],
    db: &Db,
    emits: &mut Vec<super::DeferredEmit>,
) -> OwgeResult<Option<UnitMissionReportBuilder>> {
    let user_id = mission.user_id.unwrap_or_default();
    let target_planet_id = mission.target_planet.map(|p| p as u64);

    let continue_mission =
        attack::trigger_attack_if_required(conn, mission, MissionType::Gather, db, emits).await?;
    if !continue_mission {
        return Ok(None);
    }

    ReturnMissionRegistrationBo::register_return_mission(conn, mission, None).await?;

    // sum(charge * count) over the involved units (ObjectUtils.firstNonNull(charge, 0)).
    let mut gathered: u64 = 0;
    for ou in involved_units {
        let charge: Option<Option<u16>> =
            sqlx::query_scalar("SELECT charge FROM units WHERE id = ?")
                .bind(ou.unit_id)
                .fetch_optional(&mut *conn)
                .await?;
        let charge = charge.flatten().unwrap_or(0) as u64;
        gathered += charge * ou.count;
    }

    let rational_richness = match target_planet_id {
        Some(id) => find_rational_richness(conn, id).await?,
        None => 0.0,
    };
    let with_planet_richness = gathered as f64 * rational_richness;

    // improvementBo.findUserImprovement(user).getMoreChargeCapacity() as rational.
    let grouped = UserImprovementBo::find_user_improvement(db, user_id).await?;
    let with_user_improvement =
        with_planet_richness + with_planet_richness * (grouped.more_charge_capacity / 100.0);

    // The faction percentages are Java `Float`, so Java computes `percentage / 100`
    // in f32 (0.6f != 0.6) and only then promotes to double for the multiply.
    // Reproduce the f32 division to match Java's gathered amounts bit-for-bit.
    let (custom_primary, custom_secondary) = find_faction_custom_gather(conn, user_id).await?;
    let (primary_resource, secondary_resource) = if custom_primary > 0.0 && custom_secondary > 0.0 {
        (
            with_user_improvement * ((custom_primary / 100.0_f32) as f64),
            with_user_improvement * ((custom_secondary / 100.0_f32) as f64),
        )
    } else {
        (with_user_improvement * 0.5, with_user_improvement * 0.5)
    };

    // user.addtoPrimary / addToSecondary.
    sqlx::query(
        "UPDATE user_storage \
            SET primary_resource = primary_resource + ?, \
                secondary_resource = secondary_resource + ? \
          WHERE id = ?",
    )
    .bind(primary_resource)
    .bind(secondary_resource)
    .bind(user_id)
    .execute(&mut *conn)
    .await?;

    let builder = create_report_base(conn, mission, involved_units)
        .await?
        .with_gather_information(primary_resource, secondary_resource);

    // socketIoService.sendMessage(user, "mission_gather_result", {primary, secondary})
    // — deferred to post-commit (one-time push).
    emits.push(super::DeferredEmit::GatherResult {
        user_id,
        primary: primary_resource,
        secondary: secondary_resource,
    });
    // mission.setResolved(true) is persisted by the report save path.
    Ok(Some(builder))
}

/// `Planet.findRationalRichness` — `richness / 100`.
async fn find_rational_richness(
    conn: &mut MySqlConnection,
    planet_id: u64,
) -> OwgeResult<f64> {
    let richness: Option<u16> =
        sqlx::query_scalar("SELECT richness FROM planets WHERE id = ?")
            .bind(planet_id)
            .fetch_optional(&mut *conn)
            .await?;
    Ok(richness.unwrap_or(0) as f64 / 100.0)
}

/// The user's faction custom gather percentages (`custom_primary_gather_percentage`,
/// `custom_secondary_gather_percentage`); both are `float UNSIGNED` nullable.
async fn find_faction_custom_gather(
    conn: &mut MySqlConnection,
    user_id: i32,
) -> OwgeResult<(f32, f32)> {
    let row: Option<(Option<f32>, Option<f32>)> = sqlx::query_as(
        "SELECT f.custom_primary_gather_percentage, f.custom_secondary_gather_percentage \
           FROM user_storage us JOIN factions f ON f.id = us.faction \
          WHERE us.id = ?",
    )
    .bind(user_id)
    .fetch_optional(&mut *conn)
    .await?;
    let (p, s) = row.unwrap_or((None, None));
    // Kept as f32 (Java `Float`) so the caller can reproduce Java's f32 `/100`.
    Ok((p.unwrap_or(0.0), s.unwrap_or(0.0)))
}
