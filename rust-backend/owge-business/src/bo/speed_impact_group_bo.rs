//! Port of (the read side of) `SpeedImpactGroupBo` and
//! `speedimpactgroup.UnlockedSpeedImpactGroupService`.
//!
//! The `speed_impact_group_unlocked_change` sync payload
//! (`UnlockedSpeedImpactGroupService.findCrossGalaxyUnlocked`) returns the ids
//! (a `List<Integer>`) of the speed impact groups *unlocked* for the user,
//! resolved entirely through the unlock engine (`UnlockedRelationBo` over
//! `ObjectEnum.SPEED_IMPACT_GROUP`).

use crate::db::Db;
use crate::dto::{SpeedImpactGroupDto, SpeedImpactGroupInput};
use crate::error::{OwgeError, OwgeResult};

const ANY: &str = "ANY";

/// A speed impact group row joined with its (optional) image, with exact SQL
/// column types so sqlx decode never panics on signedness/width.
#[derive(sqlx::FromRow)]
struct SpeedImpactGroupRow {
    id: u16,
    name: String,
    is_fixed: i8,
    mission_explore: f64,
    mission_gather: f64,
    mission_establish_base: f64,
    mission_attack: f64,
    mission_conquest: f64,
    mission_counterattack: f64,
    can_explore: Option<String>,
    can_gather: Option<String>,
    can_establish_base: Option<String>,
    can_attack: Option<String>,
    can_counterattack: Option<String>,
    can_conquest: Option<String>,
    can_deploy: Option<String>,
    image: Option<u64>,
    image_url: Option<String>,
}

impl From<SpeedImpactGroupRow> for SpeedImpactGroupDto {
    fn from(r: SpeedImpactGroupRow) -> Self {
        SpeedImpactGroupDto {
            id: r.id,
            name: r.name,
            is_fixed: r.is_fixed != 0,
            mission_explore: r.mission_explore,
            mission_gather: r.mission_gather,
            mission_establish_base: r.mission_establish_base,
            mission_attack: r.mission_attack,
            mission_conquest: r.mission_conquest,
            mission_counterattack: r.mission_counterattack,
            can_explore: r.can_explore.unwrap_or_else(|| ANY.to_string()),
            can_gather: r.can_gather.unwrap_or_else(|| ANY.to_string()),
            can_establish_base: r.can_establish_base.unwrap_or_else(|| ANY.to_string()),
            can_attack: r.can_attack.unwrap_or_else(|| ANY.to_string()),
            can_counterattack: r.can_counterattack.unwrap_or_else(|| ANY.to_string()),
            can_conquest: r.can_conquest.unwrap_or_else(|| ANY.to_string()),
            can_deploy: r.can_deploy.unwrap_or_else(|| ANY.to_string()),
            image: r.image,
            image_url: r.image_url,
        }
    }
}

// `imageUrl` is a Java `@Transient` field computed by `ImageStoreListener`
// (base URL + `images_store.filename`); the base URL is a config value, so it
// is left NULL here and resolved when the image domain lands (M2). Only the raw
// `image_id` is selected.
const SELECT_DTO: &str = "\
    SELECT s.id, s.name, s.is_fixed, \
           s.mission_explore, s.mission_gather, s.mission_establish_base, \
           s.mission_attack, s.mission_conquest, s.mission_counterattack, \
           s.can_explore, s.can_gather, s.can_establish_base, s.can_attack, \
           s.can_counterattack, s.can_conquest, s.can_deploy, \
           s.image_id AS image, NULL AS image_url \
    FROM speed_impact_groups s ";

pub struct SpeedImpactGroupBo;

impl SpeedImpactGroupBo {
    /// Every speed impact group as a DTO — the basis for the unlocked-groups
    /// handler once the unlock engine is ported.
    pub async fn find_all_dtos(db: &Db) -> OwgeResult<Vec<SpeedImpactGroupDto>> {
        let rows = sqlx::query_as::<_, SpeedImpactGroupRow>(&format!("{SELECT_DTO} ORDER BY s.id"))
            .fetch_all(db)
            .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `WithReadRestServiceTrait.findOneById` for the admin CRUD.
    pub async fn find_by_id(db: &Db, id: u16) -> OwgeResult<Option<SpeedImpactGroupDto>> {
        let row = sqlx::query_as::<_, SpeedImpactGroupRow>(&format!("{SELECT_DTO} WHERE s.id = ?"))
            .bind(id)
            .fetch_optional(db)
            .await?;
        Ok(row.map(Into::into))
    }

    /// `CrudRestServiceTrait.saveNew` — `speed_impact_groups.id` is AUTO_INCREMENT.
    /// Mirrors `beforeSave`: a `null` image clears the column, otherwise the image
    /// id is stored as-is (the existence check is the FK's job here).
    pub async fn save_new(db: &Db, input: &SpeedImpactGroupInput) -> OwgeResult<SpeedImpactGroupDto> {
        let result = sqlx::query(
            "INSERT INTO speed_impact_groups \
                (name, is_fixed, mission_explore, mission_gather, mission_establish_base, \
                 mission_attack, mission_conquest, mission_counterattack, \
                 can_explore, can_gather, can_establish_base, can_attack, \
                 can_counterattack, can_conquest, can_deploy, image_id) \
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        )
        .bind(&input.name)
        .bind(input.is_fixed as i8)
        .bind(input.mission_explore)
        .bind(input.mission_gather)
        .bind(input.mission_establish_base)
        .bind(input.mission_attack)
        .bind(input.mission_conquest)
        .bind(input.mission_counterattack)
        .bind(&input.can_explore)
        .bind(&input.can_gather)
        .bind(&input.can_establish_base)
        .bind(&input.can_attack)
        .bind(&input.can_counterattack)
        .bind(&input.can_conquest)
        .bind(&input.can_deploy)
        .bind(input.image)
        .execute(db)
        .await?;
        let id = result.last_insert_id() as u16;
        Self::find_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::Common("Speed impact group vanished after insert".into()))
    }

    /// `CrudRestServiceTrait.saveExisting` — update by id.
    pub async fn save_existing(
        db: &Db,
        id: u16,
        input: &SpeedImpactGroupInput,
    ) -> OwgeResult<SpeedImpactGroupDto> {
        let affected = sqlx::query(
            "UPDATE speed_impact_groups SET \
                name = ?, is_fixed = ?, mission_explore = ?, mission_gather = ?, \
                mission_establish_base = ?, mission_attack = ?, mission_conquest = ?, \
                mission_counterattack = ?, can_explore = ?, can_gather = ?, \
                can_establish_base = ?, can_attack = ?, can_counterattack = ?, \
                can_conquest = ?, can_deploy = ?, image_id = ? \
             WHERE id = ?",
        )
        .bind(&input.name)
        .bind(input.is_fixed as i8)
        .bind(input.mission_explore)
        .bind(input.mission_gather)
        .bind(input.mission_establish_base)
        .bind(input.mission_attack)
        .bind(input.mission_conquest)
        .bind(input.mission_counterattack)
        .bind(&input.can_explore)
        .bind(&input.can_gather)
        .bind(&input.can_establish_base)
        .bind(&input.can_attack)
        .bind(&input.can_counterattack)
        .bind(&input.can_conquest)
        .bind(&input.can_deploy)
        .bind(input.image)
        .bind(id)
        .execute(db)
        .await?
        .rows_affected();
        if affected == 0 {
            return Err(OwgeError::NotFound(format!(
                "No speed impact group with id {id}"
            )));
        }
        Self::find_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No speed impact group with id {id}")))
    }

    /// `WithDeleteRestServiceTrait.delete`.
    pub async fn delete(db: &Db, id: u16) -> OwgeResult<()> {
        sqlx::query("DELETE FROM speed_impact_groups WHERE id = ?")
            .bind(id)
            .execute(db)
            .await?;
        Ok(())
    }

    /// `UnlockedSpeedImpactGroupService.findCrossGalaxyUnlocked` — the
    /// `speed_impact_group_unlocked_change` sync payload: the speed impact
    /// groups unlocked for the user, resolved through the unlock engine
    /// (`unlocked_relation -> object_relations(SPEED_IMPACT_GROUP)`).
    pub async fn find_cross_galaxy_unlocked(db: &Db, user_id: i32) -> OwgeResult<Vec<i16>> {
        // Java `UnlockedSpeedImpactGroupService.findCrossGalaxyUnlocked` returns a
        // bare `List<Integer>` of the unlocked SPEED_IMPACT_GROUP ids (it does NOT
        // filter to cross-galaxy-capable groups despite the name).
        crate::bo::unlocked_relation_bo::UnlockedRelationBo::find_unlocked_reference_ids(
            db,
            user_id,
            crate::model::object_relation::object_enum::SPEED_IMPACT_GROUP,
        )
        .await
    }
}
