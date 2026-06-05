//! Mirrors `RuleDto` (and the descriptor DTOs) from
//! `com.kevinguanchedarias.owgejava.dto.rule`.

use serde::{Deserialize, Serialize};

use crate::dto::RequirementInformationDto;

/// Mirrors `RuleDto`. Built from a `rules` row.
///
/// `extraArgs` is the `#`-delimited `extra_args` column split into a list of
/// strings (matching `RuleEntityToDtoConverter`). `originId` / `destinationId`
/// are exposed as the Java `Long` (serialized as JSON numbers) even though the
/// underlying columns are signed `smallint`. `requirements` is declared on the
/// Java DTO but never populated by the converter, so it is always `null`.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RuleDto {
    pub id: u16,
    pub r#type: String,
    pub origin_type: String,
    pub origin_id: i64,
    pub destination_type: String,
    pub destination_id: i64,
    pub extra_args: Vec<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub requirements: Option<Vec<RequirementInformationDto>>,
}

/// Save body for `POST admin/rules` (`RuleBo.save`). The Java controller takes a
/// full `RuleDto`; `id` is optional (absent/0 = insert, present = update). The
/// `extraArgs` items are stringified and joined with `#`, dropping any item that
/// itself contains a `#` (matching `RuleDtoToEntityConverter`).
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RuleInput {
    #[serde(default)]
    pub id: Option<u16>,
    pub r#type: String,
    pub origin_type: String,
    pub origin_id: i64,
    pub destination_type: String,
    pub destination_id: i64,
    #[serde(default)]
    pub extra_args: Vec<serde_json::Value>,
}

impl RuleInput {
    /// Replicates `RuleDtoToEntityConverter.convertExtraArgs`: stringify every
    /// item, drop those containing the `#` delimiter, and join with `#`.
    pub fn joined_extra_args(&self) -> String {
        self.extra_args
            .iter()
            .map(json_value_to_string)
            .filter(|s| !s.contains('#'))
            .collect::<Vec<_>>()
            .join("#")
    }
}

/// `Object::toString()` equivalent for the JSON values the frontend sends as
/// extra args: bare scalars become their plain text, not their JSON encoding.
fn json_value_to_string(value: &serde_json::Value) -> String {
    match value {
        serde_json::Value::String(s) => s.clone(),
        serde_json::Value::Null => "null".to_string(),
        other => other.to_string(),
    }
}

/// Mirrors `RuleTypeDescriptorDto` — the list of extra-arg descriptors a rule
/// type accepts.
///
/// The Java DTO declares `List<IdNameDto> extraArgs`, but the only provider that
/// sets it (`UnitCaptureRuleTypeProviderBo`) populates it with `RuleExtraArgDto`
/// subclasses (adding a `formType` field), so the serialized items carry that
/// extra key. Every other provider leaves `extraArgs` unset; with the global
/// Jackson `Include.NON_NULL` policy a `null` list is omitted from the JSON
/// entirely, so this is `Option` with `skip_serializing_if`.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RuleTypeDescriptorDto {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extra_args: Option<Vec<RuleExtraArgDto>>,
}

/// Mirrors `RuleItemTypeDescriptorDto` — the selectable items of an item type.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RuleItemTypeDescriptorDto {
    pub items: Vec<IdNameDto>,
}

/// Mirrors `IdNameDto` (`dto.base.IdNameDto`).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct IdNameDto {
    pub id: i64,
    pub name: String,
}

/// Mirrors `RuleExtraArgDto` (`dto.rule.RuleExtraArgDto extends IdNameDto`): an
/// `IdNameDto` plus a `formType` (e.g. `"number"`) describing how the admin UI
/// should render the extra-arg input.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RuleExtraArgDto {
    pub id: i64,
    pub name: String,
    pub form_type: String,
}
