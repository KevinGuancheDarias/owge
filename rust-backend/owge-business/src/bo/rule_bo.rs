//! Port of `RuleBo` (`business.rule.RuleBo`) backing `AdminRuleRestService`
//! (`admin/rules`).
//!
//! Ported here: the data-access methods used by the controller —
//! `findByOriginTypeAndOriginId`, `findByType`, `save` and `deleteById`. The
//! `findTypeDescriptor` / `findItemTypeDescriptor` endpoints resolve their
//! answer from Spring-discovered `RuleTypeProvider` / `RuleItemTypeProvider`
//! beans (e.g. the time-special and unit rule providers); that provider
//! registry has not been ported, so those two endpoints are left returning 501
//! at the route layer.
//!
//! `origin_id` / `destination_id` are signed `smallint` columns; the DTO keeps
//! the Java `Long` width (`i64`) and the binds narrow to `i16`.
//!
//! The `findTypeDescriptor` / `findItemTypeDescriptor` endpoints resolve their
//! answer from the Spring-discovered `RuleTypeProvider` / `RuleItemTypeProvider`
//! beans. Those descriptors are static metadata baked into each provider, so the
//! registry is reimplemented here as a `match` over the requested id: the rule
//! type providers (`TimeSpecialIsActive*`, `UnitStoresUnit`, `UnitCapture`)
//! return a constant descriptor; the single item-type provider (`UNIT`) reads
//! the unit catalog from the DB. An unknown id mirrors Java's
//! `SgtBackendInvalidInputException` (`OwgeError::InvalidInput`, HTTP 400).

use std::collections::HashMap;

use crate::dto::rule::{
    IdNameDto, RuleDto, RuleExtraArgDto, RuleInput, RuleItemTypeDescriptorDto,
    RuleTypeDescriptorDto, RuleWithRelatedUnitsDto, UnitCommonDto,
};
use crate::error::{OwgeError, OwgeResult};
use crate::model::rule::Rule;
use sqlx::MySqlConnection;

/// Matches `RuleBo.ARGS_DELIMITER`.
pub const ARGS_DELIMITER: char = '#';

impl From<Rule> for RuleDto {
    fn from(r: Rule) -> Self {
        // `RuleEntityToDtoConverter` splits the raw extra_args by '#'. Java's
        // `String.split` on an empty string yields a single empty element, so a
        // missing/empty column becomes `[""]`.
        let extra_args = r
            .extra_args
            .unwrap_or_default()
            .split(ARGS_DELIMITER)
            .map(|s| s.to_string())
            .collect();
        RuleDto {
            id: r.id,
            r#type: r.r#type,
            origin_type: r.origin_type,
            origin_id: r.origin_id as i64,
            destination_type: r.destination_type,
            destination_id: r.destination_id as i64,
            extra_args,
            requirements: None,
        }
    }
}

const SELECT_COLUMNS: &str = "SELECT id, type, origin_type, origin_id, destination_type, destination_id, extra_args FROM rules";

pub struct RuleBo;

impl RuleBo {
    /// `RuleBo.findByOriginTypeAndOriginId`.
    pub async fn find_by_origin_type_and_origin_id(
        conn: &mut MySqlConnection,
        origin_type: &str,
        origin_id: i64,
    ) -> OwgeResult<Vec<RuleDto>> {
        let rows = sqlx::query_as::<_, Rule>(&format!(
            "{SELECT_COLUMNS} WHERE origin_type = ? AND origin_id = ? ORDER BY id"
        ))
        .bind(origin_type)
        .bind(origin_id as i16)
        .fetch_all(&mut *conn)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `RuleBo.findByType`.
    pub async fn find_by_type(
        conn: &mut MySqlConnection,
        rule_type: &str,
    ) -> OwgeResult<Vec<RuleDto>> {
        let rows =
            sqlx::query_as::<_, Rule>(&format!("{SELECT_COLUMNS} WHERE type = ? ORDER BY id"))
                .bind(rule_type)
                .fetch_all(&mut *conn)
                .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `RuleBo.findTypeDescriptor` — resolves the static `RuleTypeProvider`
    /// descriptor for `rule_type`. The provider ids and descriptor contents are
    /// the constants defined on the `RuleTypeProvider` beans; an unknown id
    /// matches Java's `SgtBackendInvalidInputException`.
    pub async fn find_type_descriptor(
        _conn: &mut MySqlConnection,
        rule_type: &str,
    ) -> OwgeResult<RuleTypeDescriptorDto> {
        match rule_type {
            // TimeSpecialIsActiveTemporalUnitsTypeProviderBo
            "TIME_SPECIAL_IS_ENABLED_TEMPORAL_UNITS"
            // TimeSpecialIsActiveSwapSpeedImpactGroupProviderBo
            | "TIME_SPECIAL_IS_ENABLED_DO_SWAP_SPEED_IMPACT_GROUP"
            // TimeSpecialIsActiveHideUnitsTypeProviderBo
            | "TIME_SPECIAL_IS_ENABLED_DO_HIDE"
            // UnitStoresUnitRuleTypeProviderBo
            | "UNIT_STORES_UNIT" => Ok(RuleTypeDescriptorDto { extra_args: None }),
            // UnitCaptureRuleTypeProviderBo
            "UNIT_CAPTURE" => Ok(RuleTypeDescriptorDto {
                extra_args: Some(vec![
                    RuleExtraArgDto {
                        id: 1,
                        name: "CRUD.RULES.ARG.UNIT_CAPTURE_PROBABILITY".to_string(),
                        form_type: "number".to_string(),
                    },
                    RuleExtraArgDto {
                        id: 2,
                        name: "CRUD.RULES.ARG.UNIT_CAPTURE_PERCENTAGE".to_string(),
                        form_type: "number".to_string(),
                    },
                ]),
            }),
            other => Err(OwgeError::InvalidInput(format!("No type {other} exists"))),
        }
    }

    /// `RuleBo.findItemTypeDescriptor` — resolves the `RuleItemTypeProvider`
    /// descriptor for `item_type`. The only provider is `UNIT`
    /// (`UnitRuleItemTypeProviderBo`), which lists the whole unit catalog as
    /// `id`/`name` items (`unitRepository.findAll()`, natural primary-key order).
    /// An unknown id matches Java's `SgtBackendInvalidInputException`.
    pub async fn find_item_type_descriptor(
        conn: &mut MySqlConnection,
        item_type: &str,
    ) -> OwgeResult<RuleItemTypeDescriptorDto> {
        match item_type {
            "UNIT" => {
                let items =
                    sqlx::query_as::<_, (u16, String)>("SELECT id, name FROM units ORDER BY id")
                        .fetch_all(&mut *conn)
                        .await?
                        .into_iter()
                        .map(|(id, name)| IdNameDto {
                            id: id as i64,
                            name,
                        })
                        .collect();
                Ok(RuleItemTypeDescriptorDto { items })
            }
            other => Err(OwgeError::InvalidInput(format!(
                "No item type {other} exists"
            ))),
        }
    }

    /// Read one rule by id (used internally to return the saved entity).
    pub async fn find_by_id(conn: &mut MySqlConnection, id: u16) -> OwgeResult<Option<RuleDto>> {
        let row = sqlx::query_as::<_, Rule>(&format!("{SELECT_COLUMNS} WHERE id = ?"))
            .bind(id)
            .fetch_optional(&mut *conn)
            .await?;
        Ok(row.map(Into::into))
    }

    /// `RuleBo.save` — `rules.id` is AUTO_INCREMENT. An input without an id (or
    /// id 0) inserts; otherwise it updates the existing row.
    pub async fn save(conn: &mut MySqlConnection, input: &RuleInput) -> OwgeResult<RuleDto> {
        let extra_args = input.joined_extra_args();
        match input.id.filter(|&id| id != 0) {
            None => {
                let result = sqlx::query(
                    "INSERT INTO rules \
                        (type, origin_type, origin_id, destination_type, destination_id, extra_args) \
                     VALUES (?, ?, ?, ?, ?, ?)",
                )
                .bind(&input.r#type)
                .bind(&input.origin_type)
                .bind(input.origin_id as i16)
                .bind(&input.destination_type)
                .bind(input.destination_id as i16)
                .bind(&extra_args)
                .execute(&mut *conn)
                .await?;
                let id = result.last_insert_id() as u16;
                Self::find_by_id(&mut *conn, id)
                    .await?
                    .ok_or_else(|| OwgeError::Common("Rule vanished right after insert".into()))
            }
            Some(id) => {
                sqlx::query(
                    "UPDATE rules SET type = ?, origin_type = ?, origin_id = ?, \
                        destination_type = ?, destination_id = ?, extra_args = ? WHERE id = ?",
                )
                .bind(&input.r#type)
                .bind(&input.origin_type)
                .bind(input.origin_id as i16)
                .bind(&input.destination_type)
                .bind(input.destination_id as i16)
                .bind(&extra_args)
                .bind(id)
                .execute(&mut *conn)
                .await?;
                Self::find_by_id(&mut *conn, id)
                    .await?
                    .ok_or_else(|| OwgeError::NotFound(format!("No rule with id {id}")))
            }
        }
    }

    /// `RuleBo.deleteById`.
    pub async fn delete_by_id(conn: &mut MySqlConnection, id: u16) -> OwgeResult<()> {
        sqlx::query("DELETE FROM rules WHERE id = ?")
            .bind(id)
            .execute(&mut *conn)
            .await?;
        Ok(())
    }

    /// All rules as DTOs — backing `OpenWebsocketSyncRestService.findRules`.
    pub async fn find_all(conn: &mut MySqlConnection) -> OwgeResult<Vec<RuleDto>> {
        let rows = sqlx::query_as::<_, Rule>(SELECT_COLUMNS)
            .fetch_all(&mut *conn)
            .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `OpenWebsocketSyncRestService.findRules` — all rules plus the units that
    /// appear as origin or destination (`origin_type = "UNIT"` /
    /// `destination_type = "UNIT"`), keyed by unit id.
    pub async fn find_all_with_related_units(
        conn: &mut MySqlConnection,
    ) -> OwgeResult<RuleWithRelatedUnitsDto> {
        let rules = Self::find_all(&mut *conn).await?;

        let unit_ids: std::collections::HashSet<i64> = rules
            .iter()
            .flat_map(|r| {
                let mut ids = Vec::new();
                if r.origin_type == "UNIT" {
                    ids.push(r.origin_id);
                }
                if r.destination_type == "UNIT" {
                    ids.push(r.destination_id);
                }
                ids
            })
            .collect();

        let related_units: HashMap<String, UnitCommonDto> = if unit_ids.is_empty() {
            HashMap::new()
        } else {
            let ids: Vec<i64> = unit_ids.into_iter().collect();
            let mut qb =
                sqlx::QueryBuilder::new("SELECT id, name, description FROM units WHERE id IN (");
            let mut sep = qb.separated(", ");
            for &id in &ids {
                sep.push_bind(id as i16);
            }
            qb.push(")");
            qb.build_query_as::<UnitSimpleRow>()
                .fetch_all(&mut *conn)
                .await?
                .into_iter()
                .map(|r| {
                    (
                        r.id.to_string(),
                        UnitCommonDto {
                            id: r.id,
                            name: r.name,
                            description: r.description,
                        },
                    )
                })
                .collect()
        };

        Ok(RuleWithRelatedUnitsDto {
            rules,
            related_units,
        })
    }
}

#[derive(sqlx::FromRow)]
struct UnitSimpleRow {
    id: u16,
    name: String,
    description: Option<String>,
}
