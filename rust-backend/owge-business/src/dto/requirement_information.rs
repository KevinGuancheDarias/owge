use serde::{Deserialize, Serialize};

/// Mirrors `ObjectRelationDto`.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ObjectRelationDto {
    pub id: u16,
    pub object_code: String,
    pub reference_id: i16,
}

/// Mirrors `RequirementDto`.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RequirementDto {
    pub id: i16,
    pub code: String,
    pub description: String,
}

/// Mirrors `RequirementInformationDto` — one requirement attached to an
/// `object_relation` (the admin `{id}/requirements` sub-resource payload).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RequirementInformationDto {
    pub id: i16,
    pub relation: ObjectRelationDto,
    pub requirement: RequirementDto,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub second_value: Option<i64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub third_value: Option<i64>,
}

/// `POST {id}/requirements` request body — the inbound `RequirementInformationDto`.
///
/// The controller overwrites `relation` with `(getObject(), id)` before calling
/// `addRequirementFromDto`, so the client-sent `relation` is ignored; only
/// `requirement.code`, `secondValue`, and `thirdValue` are read. Mirrors the
/// Java validation (`requirement` and `secondValue` are mandatory; `id` must be
/// null).
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RequirementInformationInput {
    #[serde(default)]
    pub requirement: Option<RequirementCodeInput>,
    #[serde(default)]
    pub second_value: Option<i64>,
    #[serde(default)]
    pub third_value: Option<i64>,
}

/// The `requirement` sub-object of the request body — only `code` is consumed.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RequirementCodeInput {
    #[serde(default)]
    pub code: Option<String>,
}

/// Mirrors `RequirementGroupDto` — a named group of requirements attached to an
/// entity (the `{id}/requirement-group` sub-resource payload).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RequirementGroupDto {
    pub id: u16,
    pub name: Option<String>,
    pub requirements: Vec<RequirementInformationDto>,
}

/// `POST {id}/requirement-group` request body — only `name` and the optional
/// initial `requirements` list are consumed by `RequirementGroupBo.add`.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RequirementGroupInput {
    #[serde(default)]
    pub name: Option<String>,
    #[serde(default)]
    pub requirements: Vec<RequirementInformationInput>,
}
