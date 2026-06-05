//! `rest/admin/*` — endpoints behind the admin-JWT filter. Every handler asks
//! for the [`AdminUser`](crate::auth::AdminUser) extractor, the Rust analogue of
//! the `adminBootJwtAuthenticationFilter` chain.
//!
//! Most admin controllers in the Java backend are thin `CrudRestServiceTrait`
//! implementations exposing the standard `GET ''`/`GET '{id}'`/`POST ''`/
//! `PUT '{id}'`/`DELETE '{id}'` routes. `galaxy` is the reference port of that
//! shape; the rest follow the same pattern.

pub mod admin_user;
pub mod attack_rule;
pub mod configuration;
pub mod critical_attack;
pub mod debug;
pub mod faction;
pub mod galaxy;
pub mod game_users;
pub mod image_store;
pub mod rule;
pub mod special_location;
pub mod speed_impact_group;
pub mod system_message;
pub mod translatable;
pub mod tutorial_section;
pub mod unit;
pub mod unit_type;
pub mod upgrade;
pub mod upgrade_type;

use axum::Router;

use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .merge(galaxy::routes())
        .merge(unit::routes())
        .merge(unit_type::routes())
        .merge(upgrade::routes())
        .merge(upgrade_type::routes())
        .merge(faction::routes())
        .merge(special_location::routes())
        .merge(attack_rule::routes())
        .merge(critical_attack::routes())
        .merge(rule::routes())
        .merge(image_store::routes())
        .merge(translatable::routes())
        .merge(tutorial_section::routes())
        .merge(configuration::routes())
        .merge(system_message::routes())
        .merge(speed_impact_group::routes())
        .merge(admin_user::routes())
        .merge(game_users::routes())
        .merge(debug::routes())
}
