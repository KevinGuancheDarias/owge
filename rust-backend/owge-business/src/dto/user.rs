use crate::OwgeError;
use crate::dto::{FactionDto, PlanetDto, UserImprovementDto};
use crate::error::OwgeResult;
use serde::Serialize;
use sqlx::{FromRow, MySqlConnection};
use sqlx_template::MysqlTemplate;

/// Mirrors `SimpleUserDataDto` (a Java record `{id, username, email}`).
#[derive(Debug, Clone, Serialize, FromRow, MysqlTemplate)]
#[table("user_storage")]
#[tp_select_builder]
#[serde(rename_all = "camelCase")]
pub struct SimpleUserData {
    #[auto]
    pub id: i32,
    pub username: String,
    pub email: String,

    #[sqlx(default)]
    pub alliance_id: Option<u16>,
}

impl SimpleUserData {
    pub async fn find_by_id(conn: &mut MySqlConnection, id: &i32) -> OwgeResult<Self> {
        Self::builder_select()
            .id(id)?
            .find_one(&mut *conn)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No user with id {id}")))
    }

    pub fn require_alliance(&self) -> OwgeResult<u16> {
        self.alliance_id
            .ok_or_else(|| OwgeError::InvalidInput("You don't have an alliance".into()))
    }

    pub fn check_no_alliance(&self) -> OwgeResult<()> {
        if self.alliance_id.is_none() {
            return Ok(());
        }

        Err(OwgeError::InvalidInput(
            "You already have an alliance, leave it first".into(),
        ))
    }
}

/// Mirrors `UserStorageDto` (the `user_data_change` sync payload). The nested
/// faction/home-planet/alliance/improvements objects are populated as their
/// domains land; the directly-stored and computed-resource fields are complete.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UserData {
    pub can_alter_twitch_state: bool,
    pub consumed_energy: f64,
    pub email: String,

    #[serde(rename = "factionDto")]
    pub faction: FactionDto,

    pub has_skipped_tutorial: bool,

    #[serde(rename = "homePlanetDto")]
    pub home_planet: PlanetDto,

    pub id: i32,
    pub improvements: UserImprovementDto,
    pub max_energy: f64,
    pub primary_resource: Option<f64>,
    pub secondary_resource: Option<f64>,
    pub username: String,
}
