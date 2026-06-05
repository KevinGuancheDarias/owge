use serde::{Deserialize, Serialize};

use super::UserStorageDto;

/// Mirrors `AllianceDto` (`CommonDto { id, name, description }` + `image` from
/// `CommonEntityWithImage` + `owner`). The Java DTO annotates `owner` with
/// `@JsonIdentityReference(alwaysAsId = true)`, so it serializes as the owner's
/// id (a number), not a nested object — we model it directly as the owner
/// `user_storage` id.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AllianceDto {
    /// `0`/absent on create (the frontend omits it); present on update. We
    /// treat `0` as "no id" — the column is `smallint unsigned` so real ids
    /// start at 1.
    #[serde(default)]
    pub id: u16,
    #[serde(default)]
    pub name: String,
    #[serde(default)]
    pub description: Option<String>,
    #[serde(default)]
    pub image: Option<String>,
    /// Serialized as the owner's id only (Jackson `alwaysAsId`). Ignored as an
    /// input — `save` always derives the owner from the invoker / stored row.
    #[serde(default)]
    pub owner: i32,
}

/// Mirrors `AllianceJoinRequestDto` (`{ id, user, alliance }`).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AllianceJoinRequestDto {
    pub id: u32,
    pub user: UserStorageDto,
    pub alliance: AllianceDto,
}

/// Body of `POST game/alliance/requestJoin` (`{ "allianceId": <int> }`).
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RequestJoinBody {
    pub alliance_id: u16,
}

/// Body of `acceptJoinRequest` / `rejectJoinRequest` (`{ "joinRequestId": <int> }`).
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct JoinRequestIdBody {
    pub join_request_id: u32,
}
