//! `rest/game/*` — endpoints behind the game-JWT filter.

pub mod alliance;
pub mod mission;
pub mod unit;
pub mod upgrade;

use axum::extract::{Query, State};
use axum::http::StatusCode;
use axum::routing::{get, post};
use axum::{Json, Router};
use owge_business::bo::admin_user_bo::TokenPojo;
use owge_business::bo::{
    AdminUserBo, ConfigurationBo, FactionBo, GalaxyBo, MissionReportBo, PlanetListBo, RankingBo,
    SystemMessageBo, TimeSpecialBo, TrackBrowserBo, TutorialBo, UserStorageBo,
};
use owge_business::dto::{
    FactionDto, GalaxyDto, PlanetDto, RankingEntryDto, TimeSpecialDto, TutorialSectionEntryDto,
};
use owge_business::websocket;
use serde::{Deserialize, Serialize};
use serde_json::{Map, Value};

use crate::auth::GameUser;
use crate::http_error::ApiResult;
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/game/adminLogin", post(admin_login))
        .route("/game/faction/findVisible", get(find_visible_factions))
        .route("/game/user/exists", get(user_exists))
        .route("/game/websocket-sync", get(websocket_sync))
        .route("/game/ranking", get(ranking))
        .route("/game/galaxy/navigate", get(galaxy_navigate))
        .route("/game/time_special", get(time_special_list))
        .route("/game/time_special/{id}", get(time_special_by_id))
        .route("/game/tutorial/entries", get(tutorial_entries))
        .route("/game/planet-list", post(planet_list_add))
        .route(
            "/game/planet-list/{planetId}",
            axum::routing::delete(planet_list_delete),
        )
        .route(
            "/game/system-message/mark-as-read",
            post(system_message_mark_read),
        )
        .route("/game/tutorial/visited-entries", post(tutorial_add_visited))
        .route("/game/track-browser/warn", post(track_browser_warn))
        .route("/game/track-browser/error", post(track_browser_error))
        .route(
            "/game/twitch-state",
            get(twitch_state_get).put(twitch_state_put),
        )
        .route("/game/report/mark-as-read", post(report_mark_as_read))
        .route(
            "/game/report/mark-as-read-before-date/{date}",
            post(report_mark_as_read_before_date),
        )
        .route("/game/report/findMy", get(report_find_my))
        .route("/game/user/subscribe", get(user_subscribe))
        .route("/game/planet/leave", post(planet_leave))
        .route("/game/time_special/activate", post(time_special_activate))
        .merge(alliance::routes())
        .merge(mission::routes())
        .merge(unit::routes())
        .merge(upgrade::routes())
}

/// `AdminLoginRestService.login` -> `AdminUserBo.login`. The caller is already
/// a valid game user; this exchanges their game token for an admin token.
async fn admin_login(
    State(state): State<AppState>,
    GameUser(user): GameUser,
) -> ApiResult<Json<TokenPojo>> {
    let token = AdminUserBo::login(&state.db, &user, &state.admin_jwt).await?;
    Ok(Json(token))
}

/// `FactionRestService.findVisible` -> `FactionBo.findVisible`.
async fn find_visible_factions(
    State(state): State<AppState>,
    _user: GameUser,
) -> ApiResult<Json<Vec<FactionDto>>> {
    let factions = FactionBo::find_visible(&state.db).await?;
    Ok(Json(factions))
}

/// `UserRestService.exists` -> `UserStorageBo.exists` — has this account user
/// subscribed to this universe?
async fn user_exists(
    State(state): State<AppState>,
    GameUser(user): GameUser,
) -> ApiResult<Json<bool>> {
    Ok(Json(
        UserStorageBo::exists(&state.db, user.id as i32).await?,
    ))
}

#[derive(Deserialize)]
struct SyncQuery {
    /// Comma-separated list of sync keys (Spring binds `?keys=a,b,c` or repeated
    /// `?keys=a&keys=b` to `List<String>`; we accept the comma form).
    keys: String,
}

/// `WebsocketSyncRestService.sync` -> `WebsocketSyncService.findWantedData` —
/// the frontend's HTTP hydration path.
async fn websocket_sync(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Query(q): Query<SyncQuery>,
) -> ApiResult<Json<Map<String, Value>>> {
    let keys: Vec<String> = q
        .keys
        .split(',')
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
        .collect();
    let data = websocket::find_wanted_data(&state.db, user.id as i32, &keys).await?;
    Ok(Json(data))
}

/// `RankingRestService.findAll` -> `RankingBo.find_ranking`.
async fn ranking(
    State(state): State<AppState>,
    _user: GameUser,
) -> ApiResult<Json<Vec<RankingEntryDto>>> {
    Ok(Json(RankingBo::find_ranking(&state.db).await?))
}

#[derive(Deserialize)]
struct NavigateQuery {
    #[serde(rename = "galaxyId")]
    galaxy_id: u16,
    sector: u32,
    quadrant: u32,
}

/// Mirrors `NavigationPojo { galaxies, planets }`.
#[derive(Serialize)]
struct NavigationPojo {
    galaxies: Vec<GalaxyDto>,
    planets: Vec<PlanetDto>,
}

/// `GalaxyRestService.navigate` -> all galaxies + the planets at the requested
/// coordinates.
async fn galaxy_navigate(
    State(state): State<AppState>,
    user: GameUser,
    Query(q): Query<NavigateQuery>,
) -> ApiResult<Json<NavigationPojo>> {
    let galaxies = GalaxyBo::find_all(&state.db).await?;
    let planets = GalaxyBo::find_planets_at(
        &state.db,
        q.galaxy_id,
        q.sector,
        q.quadrant,
        Some(user.0.id),
    )
    .await?;

    Ok(Json(NavigationPojo { galaxies, planets }))
}

/// `TimeSpecialRestService` (WithRead) GET list.
async fn time_special_list(
    State(state): State<AppState>,
    GameUser(user): GameUser,
) -> ApiResult<Json<Vec<TimeSpecialDto>>> {
    Ok(Json(
        TimeSpecialBo::find_all_dtos(&state.db, user.id as i32).await?,
    ))
}

/// `TimeSpecialRestService` (WithRead) GET one. Java's `WithReadRestServiceTrait`
/// GET `{id}` calls `findByIdOrDie`, so an absent id is a 404 — not a `200 null`.
async fn time_special_by_id(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    axum::extract::Path(id): axum::extract::Path<u16>,
) -> ApiResult<Json<TimeSpecialDto>> {
    let dto = TimeSpecialBo::find_dto_by_id(&state.db, user.id as i32, id)
        .await?
        .ok_or_else(|| {
            owge_business::OwgeError::NotFound(format!("No time special with id {id}"))
        })?;
    Ok(Json(dto))
}

/// `TutorialRestService.findEntries`.
async fn tutorial_entries(
    State(state): State<AppState>,
    _user: GameUser,
) -> ApiResult<Json<Vec<TutorialSectionEntryDto>>> {
    Ok(Json(TutorialBo::find_entries(&state.db).await?))
}

#[derive(Deserialize)]
struct PlanetListAddBody {
    #[serde(rename = "planetId")]
    planet_id: u64,
    #[serde(default)]
    name: Option<String>,
}

/// `PlanetListRestService.add`.
async fn planet_list_add(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(body): Json<PlanetListAddBody>,
) -> ApiResult<Json<&'static str>> {
    PlanetListBo::add(
        &state.db,
        user.id as i32,
        body.planet_id,
        body.name.as_deref(),
    )
    .await?;
    Ok(Json("OK"))
}

/// `PlanetListRestService.delete`.
async fn planet_list_delete(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    axum::extract::Path(planet_id): axum::extract::Path<u64>,
) -> ApiResult<Json<&'static str>> {
    PlanetListBo::delete(&state.db, user.id as i32, planet_id).await?;
    Ok(Json("OK"))
}

/// `SystemMessageRestService.markAsRead` — body is a JSON array of message ids.
async fn system_message_mark_read(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(ids): Json<Vec<u16>>,
) -> ApiResult<Json<&'static str>> {
    SystemMessageBo::mark_as_read(&state.db, user.id as i32, &ids).await?;
    Ok(Json("OK"))
}

/// `TutorialRestService.addVisitedEntry` — body is a single entry id.
async fn tutorial_add_visited(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(entry_id): Json<u32>,
) -> ApiResult<Json<&'static str>> {
    TutorialBo::add_visited_entry(&state.db, user.id as i32, entry_id).await?;
    Ok(Json("OK"))
}

/// `TrackBrowserRestService.warn` — body is the raw JSON content string.
async fn track_browser_warn(
    State(state): State<AppState>,
    _user: GameUser,
    body: String,
) -> ApiResult<StatusCode> {
    TrackBrowserBo::track(&state.db, "warn", &body).await?;
    Ok(StatusCode::OK)
}

/// `TrackBrowserRestService.error`.
async fn track_browser_error(
    State(state): State<AppState>,
    _user: GameUser,
    body: String,
) -> ApiResult<StatusCode> {
    TrackBrowserBo::track(&state.db, "error", &body).await?;
    Ok(StatusCode::OK)
}

/// `TwitchStateRestService.findTwitchState` — the `TWITCH_STATE` config flag
/// (defaulting to `false`).
async fn twitch_state_get(State(state): State<AppState>, _user: GameUser) -> ApiResult<Json<bool>> {
    let cfg = ConfigurationBo::find_or_set_default(&state.db, "TWITCH_STATE", "false").await?;
    Ok(Json(cfg.value.eq_ignore_ascii_case("true")))
}

/// `TwitchStateRestService.defineState` — only a user with
/// `can_alter_twitch_state` may flip the flag. The websocket `twitch_state_change`
/// emission is deferred to M4.
async fn twitch_state_put(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    body: String,
) -> ApiResult<StatusCode> {
    let status = body.replace('"', "");
    let status = status.trim().eq_ignore_ascii_case("true");
    let details = UserStorageBo::find_by_id(&state.db, user.id as i32).await?;
    let can_alter = details.map(|u| u.can_alter_twitch_state).unwrap_or(false);
    if !can_alter {
        return Err(owge_business::OwgeError::InvalidInput(
            "You can't get out of Matrix, the system rules your live!".into(),
        )
        .into());
    }
    ConfigurationBo::save(&state.db, "TWITCH_STATE", None, &status.to_string()).await?;
    // socketIoService.sendMessage(null, "twitch_state_change", () -> statusBool):
    // null target → broadcast (with watermark for all users); value is the boolean.
    owge_business::bo::realtime_emitter::emit_twitch_state_change(
        &state.db,
        serde_json::Value::Bool(status),
    )
    .await?;
    Ok(StatusCode::OK)
}

/// `ReportRestService.markAsRead` — body is a JSON array of report ids.
async fn report_mark_as_read(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(ids): Json<Vec<u64>>,
) -> ApiResult<StatusCode> {
    MissionReportBo::mark_as_read(&state.db, user.id as i32, &ids).await?;
    // MissionReportBo.markAsRead → emitCountChange(userId).
    owge_business::bo::realtime_emitter::emit_mission_report_count_change(
        &state.db,
        user.id as i32,
    )
    .await?;
    Ok(StatusCode::OK)
}

/// `ReportRestService.markAsReadBeforeDate` — `{date}` is an ISO-8601 instant.
async fn report_mark_as_read_before_date(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    axum::extract::Path(date): axum::extract::Path<String>,
) -> ApiResult<StatusCode> {
    let parsed = chrono::DateTime::parse_from_rfc3339(&date)
        .map(|dt| dt.naive_utc())
        .or_else(|_| chrono::NaiveDateTime::parse_from_str(&date, "%Y-%m-%dT%H:%M:%S%.f"))
        .map_err(|_| owge_business::OwgeError::InvalidInput(format!("Invalid date: {date}")))?;
    MissionReportBo::mark_as_read_before_date(&state.db, user.id as i32, parsed).await?;
    // MissionReportBo.markAsReadBeforeDate → emitCountChange(userId).
    owge_business::bo::realtime_emitter::emit_mission_report_count_change(
        &state.db,
        user.id as i32,
    )
    .await?;
    Ok(StatusCode::OK)
}

#[derive(Deserialize)]
struct FindMyQuery {
    page: i32,
}

/// `ReportRestService.findMy` (deprecated) -> `MissionReportBo
/// .findMissionReportsInformation`. The `page` query param is 1-based on the
/// wire; Java passes `page - 1` to the Bo (which is 0-based).
async fn report_find_my(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Query(q): Query<FindMyQuery>,
) -> ApiResult<Json<owge_business::dto::MissionReportResponse>> {
    let response =
        MissionReportBo::find_mission_reports_information(&state.db, user.id as i32, q.page - 1)
            .await?;
    Ok(Json(response))
}

#[derive(Deserialize)]
struct SubscribeQuery {
    #[serde(rename = "factionId")]
    faction_id: u16,
}

/// `UserRestService.subscribe` — universe provisioning: validates the faction,
/// picks a free spawn planet, seeds resources, marks the planet owned/home, and
/// fires the faction/home-galaxy requirement triggers (which unlock the player's
/// starting content). Returns the Java `boolean` result.
async fn user_subscribe(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Query(q): Query<SubscribeQuery>,
) -> ApiResult<Json<bool>> {
    Ok(Json(
        UserStorageBo::subscribe(&state.db, &user, q.faction_id).await?,
    ))
}

#[derive(Deserialize)]
struct PlanetLeaveQuery {
    /// `planets.id` = `bigint unsigned`. Spring binds `@RequestParam("planetId")`;
    /// like the other `@RequestParam` POST endpoints (unit build/cancel, upgrade
    /// register) this port takes it from the query string.
    #[serde(rename = "planetId")]
    planet_id: u64,
}

/// `PlanetRestService.leave` -> `PlanetBo.doLeavePlanet` — relinquish a
/// (non-home) owned planet. `canLeavePlanet` forbids leaving a HOME planet, a
/// planet not owned by the invoker, a planet with stationed units, or one with a
/// running BUILD_UNIT mission; failure throws `ERR_I18N_CAN_NOT_LEAVE_PLANET`
/// (HTTP 400). Returns the Java `"OK"` body.
async fn planet_leave(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Query(q): Query<PlanetLeaveQuery>,
) -> ApiResult<Json<&'static str>> {
    owge_business::bo::PlanetBo::leave_planet(&state.db, user.id as i32, q.planet_id).await?;
    Ok(Json("OK"))
}

/// `TimeSpecialRestService.activate` — activate an unlocked time special.
///
/// `@RequestBody Integer timeSpecialId` is a bare JSON number. Returns
/// `activeTimeSpecialBo.toDto(activeTimeSpecialBo.activate(id))`.
///
/// PARTIAL (see `ActiveTimeSpecialBo::activate`): the activation row + DTO are
/// faithful, but the `TIME_SPECIAL_EFFECT_END` task has no Rust runner yet (Java
/// uses Quartz, not db-scheduler), so the effect does not auto-expire, and the
/// `triggerTimeSpecialStateChange` requirement trigger + websocket emissions are
/// not fired.
async fn time_special_activate(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(time_special_id): Json<u16>,
) -> ApiResult<Json<owge_business::dto::ActiveTimeSpecialDto>> {
    let dto = owge_business::bo::ActiveTimeSpecialBo::activate(
        &state.db,
        user.id as i32,
        time_special_id,
    )
    .await?;
    Ok(Json(dto))
}
