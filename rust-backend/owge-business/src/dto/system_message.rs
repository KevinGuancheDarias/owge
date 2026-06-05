use chrono::{NaiveDateTime, NaiveTime, Utc};
use serde::{Deserialize, Deserializer, Serialize, Serializer};

/// Mirrors the Java `com.kevinguanchedarias.owgejava.pojo.SystemMessageUser` —
/// a system message decorated with the per-user read state. This is the payload
/// of the `system_message_change` sync handler (`SystemMessageBo.findReadByUser`).
///
/// Note on JSON field names: the Java pojo's boolean field is `isRead`, but with
/// Lombok `@Data` its getter is `getRead()`, so Jackson serializes the key as
/// `read` (not `isRead`). `creationDate` is serialized via
/// `@JsonFormat(shape = STRING)` (no pattern), i.e. ISO-8601 — which is exactly
/// chrono's default `NaiveDateTime` serde output.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemMessageUserDto {
    /// `smallint unsigned`.
    pub id: u16,
    pub content: String,
    pub creation_date: NaiveDateTime,
    /// Serialized as the JSON key `read` (see struct docs).
    pub read: bool,
}

/// Mirrors the Java `com.kevinguanchedarias.owgejava.dto.SystemMessageDto` — the
/// request body and response of `AdminSystemMessageRestService.create`
/// (`POST admin/system-message`).
///
/// `creationDate` carries `@JsonFormat(shape = STRING, pattern = "MM-dd-yyyy")`,
/// so it is serialized (and deserialized) as the date-only `MM-dd-yyyy` string,
/// matching the Java JSON exactly.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemMessageDto {
    /// `smallint unsigned`, AUTO_INCREMENT.
    pub id: u16,
    pub content: String,
    #[serde(serialize_with = "serialize_creation_date")]
    pub creation_date: NaiveDateTime,
}

/// Admin create request body for a system message. `id` is generated on insert;
/// `creationDate`, when omitted, defaults to "now" in UTC, mirroring the Java
/// `@Builder.Default ... LocalDateTime.now(ZoneOffset.UTC)`.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemMessageInput {
    pub content: String,
    #[serde(default, deserialize_with = "deserialize_creation_date")]
    pub creation_date: Option<NaiveDateTime>,
}

const CREATION_DATE_FORMAT: &str = "%m-%d-%Y";

fn serialize_creation_date<S>(value: &NaiveDateTime, serializer: S) -> Result<S::Ok, S::Error>
where
    S: Serializer,
{
    serializer.serialize_str(&value.format(CREATION_DATE_FORMAT).to_string())
}

/// Parses the Jackson `MM-dd-yyyy` date string into a `NaiveDateTime` at
/// midnight, matching Jackson deserializing a date-only string into a
/// `LocalDateTime`. A missing/null value yields `None`.
fn deserialize_creation_date<'de, D>(deserializer: D) -> Result<Option<NaiveDateTime>, D::Error>
where
    D: Deserializer<'de>,
{
    let opt: Option<String> = Option::deserialize(deserializer)?;
    match opt {
        None => Ok(None),
        Some(s) => {
            let date = chrono::NaiveDate::parse_from_str(&s, CREATION_DATE_FORMAT)
                .map_err(serde::de::Error::custom)?;
            Ok(Some(date.and_time(NaiveTime::MIN)))
        }
    }
}

impl SystemMessageInput {
    /// The effective creation date: the supplied value, or "now" in UTC.
    pub fn effective_creation_date(&self) -> NaiveDateTime {
        self.creation_date
            .unwrap_or_else(|| Utc::now().naive_utc())
    }
}
