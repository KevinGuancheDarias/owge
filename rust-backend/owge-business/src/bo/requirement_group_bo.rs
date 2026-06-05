//! Port of `RequirementGroupBo` and the read/delete sides of
//! `CrudWithRequirementGroupsRestServiceTrait`.
//!
//! ## Relation wiring (the intricate part)
//!
//! A requirement group is a `requirement_group` row plus an `object_relations`
//! row of type `REQUIREMENT_GROUP` (reference_id = the group id), created by the
//! Java `EntityWithRelationListener.@PostPersist` when the group is saved. The
//! group is attached to its parent entity (e.g. a speed impact group) as the
//! **slave** of an `object_relation__object_relation` whose **master** is the
//! parent entity's relation `(targetObject, referenceId)` — found-or-created.
//! Per-group requirements are `requirements_information` rows on the group's own
//! `REQUIREMENT_GROUP` relation. The unlock engine then applies OR-semantics:
//! the master unlocks when **any** slave group's requirements are met (already
//! ported in `requirement_engine.rs`).
//!
//! All of this is replicated here against the schema directly. Cache eviction
//! (`REQUIREMENT_GROUP_CACHE_TAG`) is M4 and is left as a TODO.

use crate::bo::{ObjectRelationBo, RequirementBo};
use crate::db::Db;
use crate::dto::requirement_information::{
    RequirementGroupDto, RequirementInformationInput,
};
use crate::error::OwgeResult;
use crate::model::object_relation::object_enum;

pub struct RequirementGroupBo;

impl RequirementGroupBo {
    /// `RequirementGroupBo.add` — create a requirement group attached to the
    /// parent entity `(target_object, reference_id)`, add its requirements, and
    /// link it as a slave of the parent's relation. Returns the new group's DTO.
    pub async fn add(
        db: &Db,
        target_object: &str,
        reference_id: i16,
        name: Option<&str>,
        requirements: &[RequirementInformationInput],
    ) -> OwgeResult<RequirementGroupDto> {
        // 1. Persist the requirement_group row.
        let group_id = {
            let mut tx = db.begin().await?;
            let result = sqlx::query("INSERT INTO requirement_group (name) VALUES (?)")
                .bind(name)
                .execute(&mut *tx)
                .await?;
            let group_id = result.last_insert_id() as u16;
            // 2. The @PostPersist listener creates the group's REQUIREMENT_GROUP
            //    relation (reference_id = group id).
            ObjectRelationBo::create(&mut tx, object_enum::REQUIREMENT_GROUP, group_id as i16)
                .await?;
            // 3. Find-or-create the parent entity's relation (the master).
            let master_id = ObjectRelationBo::find_object_relation_or_create(
                &mut tx,
                target_object,
                reference_id,
            )
            .await?;
            let slave_id =
                ObjectRelationBo::find_one(&mut tx, object_enum::REQUIREMENT_GROUP, group_id as i16)
                    .await?
                    .expect("group relation was just created");
            // 5. Link the group's relation as a slave of the master.
            sqlx::query(
                "INSERT INTO object_relation__object_relation \
                    (master_relation_id, slave_relation_id) VALUES (?, ?)",
            )
            .bind(master_id)
            .bind(slave_id)
            .execute(&mut *tx)
            .await?;
            tx.commit().await?;
            group_id
        };

        // 4. Add each requirement against the group's REQUIREMENT_GROUP relation
        //    (each runs in its own transaction, like the Java per-iteration save).
        for requirement in requirements {
            RequirementBo::add_requirement_from_dto(
                db,
                object_enum::REQUIREMENT_GROUP,
                group_id as i16,
                requirement,
            )
            .await?;
        }
        // No-op in the Rust port: the Java REQUIREMENT_GROUP taggable-cache
        // (`@TaggableCacheable(tags = REQUIREMENT_GROUP_CACHE_TAG)` on
        // `RequirementGroupBo.findRequirements`) is not replicated — requirement
        // group data is recomputed on demand, so there is nothing to evict.

        Self::to_dto(db, group_id).await
    }

    /// `RequirementGroupBo.doFindRequirements` + `toDto` — the groups (slaves)
    /// attached to the parent entity's relation, each with its requirements.
    /// Backs `GET {id}/requirement-group`.
    pub async fn find_groups(
        db: &Db,
        target_object: &str,
        reference_id: i16,
    ) -> OwgeResult<Vec<RequirementGroupDto>> {
        // The parent's relation; if it has none, there are no groups.
        let mut conn = db.acquire().await?;
        let Some(master_id) =
            ObjectRelationBo::find_one(&mut conn, target_object, reference_id).await?
        else {
            return Ok(Vec::new());
        };
        drop(conn);
        // Slave REQUIREMENT_GROUP relations -> the group ids (reference_id).
        let group_ids = sqlx::query_scalar::<_, i16>(
            "SELECT o.reference_id \
             FROM object_relation__object_relation oo \
             JOIN object_relations o ON o.id = oo.slave_relation_id \
             WHERE oo.master_relation_id = ? AND o.object_description = ? \
             ORDER BY o.reference_id",
        )
        .bind(master_id)
        .bind(object_enum::REQUIREMENT_GROUP)
        .fetch_all(db)
        .await?;
        let mut result = Vec::with_capacity(group_ids.len());
        for group_id in group_ids {
            result.push(Self::to_dto(db, group_id as u16).await?);
        }
        Ok(result)
    }

    /// `deleteGroup` — `RequirementGroupRepository.deleteById(groupId)`, which via
    /// the `@PreRemove`/`ObjectRelationBo.delete` cascade removes the group's
    /// `REQUIREMENT_GROUP` relation (and its `requirements_information`,
    /// `unlocked_relation`, and the `object_relation__object_relation` link rows)
    /// before deleting the `requirement_group` row.
    pub async fn delete(db: &Db, group_id: u16) -> OwgeResult<()> {
        let mut tx = db.begin().await?;
        if let Some(relation_id) =
            ObjectRelationBo::find_one(&mut tx, object_enum::REQUIREMENT_GROUP, group_id as i16)
                .await?
        {
            sqlx::query("DELETE FROM requirements_information WHERE relation_id = ?")
                .bind(relation_id)
                .execute(&mut *tx)
                .await?;
            sqlx::query("DELETE FROM unlocked_relation WHERE relation_id = ?")
                .bind(relation_id)
                .execute(&mut *tx)
                .await?;
            // The master/slave link rows cascade on the object_relations delete
            // (ON DELETE CASCADE), but delete explicitly to keep this independent
            // of FK config.
            sqlx::query(
                "DELETE FROM object_relation__object_relation \
                 WHERE master_relation_id = ? OR slave_relation_id = ?",
            )
            .bind(relation_id)
            .bind(relation_id)
            .execute(&mut *tx)
            .await?;
            sqlx::query("DELETE FROM object_relations WHERE id = ?")
                .bind(relation_id)
                .execute(&mut *tx)
                .await?;
        }
        sqlx::query("DELETE FROM requirement_group WHERE id = ?")
            .bind(group_id)
            .execute(&mut *tx)
            .await?;
        tx.commit().await?;
        // No-op in the Rust port: the Java REQUIREMENT_GROUP taggable-cache is not
        // replicated — requirement group data is recomputed on demand, so there is
        // nothing to evict.
        Ok(())
    }

    /// `RequirementGroupBo.toDto` — the group's name plus its requirements
    /// (against its own `REQUIREMENT_GROUP` relation).
    async fn to_dto(db: &Db, group_id: u16) -> OwgeResult<RequirementGroupDto> {
        let name: Option<String> =
            sqlx::query_scalar("SELECT name FROM requirement_group WHERE id = ?")
                .bind(group_id)
                .fetch_optional(db)
                .await?
                .flatten();
        let requirements =
            RequirementBo::find_requirements(db, object_enum::REQUIREMENT_GROUP, group_id as i16)
                .await?;
        Ok(RequirementGroupDto {
            id: group_id,
            name,
            requirements,
        })
    }
}
