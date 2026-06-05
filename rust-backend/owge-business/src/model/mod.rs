//! Persistence entities — `sqlx::FromRow` structs over the OWGE schema, the
//! Rust counterpart of the Java JPA `@Entity` classes under
//! `com.kevinguanchedarias.owgejava.entity`.

pub mod admin_user;
pub mod alliance;
pub mod attack_rule;
pub mod configuration;
pub mod critical_attack;
pub mod faction;
pub mod galaxy;
pub mod image_store;
pub mod mission;
pub mod object_relation;
pub mod obtained_unit;
pub mod planet;
pub mod planet_list;
pub mod ranking;
pub mod rule;
pub mod special_location;
pub mod speed_impact_group;
pub mod system_message;
pub mod time_special;
pub mod tutorial;
pub mod unit;
pub mod unit_type;
pub mod upgrade;
pub mod user_storage;

pub use admin_user::AdminUser;
pub use alliance::{Alliance, AllianceJoinRequest};
pub use attack_rule::{AttackRule, AttackRuleEntry};
pub use configuration::Configuration;
pub use critical_attack::{CriticalAttack, CriticalAttackEntry};
pub use faction::Faction;
pub use galaxy::Galaxy;
pub use image_store::ImageStore;
pub use mission::{Mission, MissionInformation, MissionReport, MissionType};
pub use object_relation::ObjectRelation;
pub use obtained_unit::ObtainedUnit;
pub use planet::Planet;
pub use planet_list::PlanetList;
pub use ranking::Ranking;
pub use rule::Rule;
pub use special_location::SpecialLocation;
pub use speed_impact_group::SpeedImpactGroup;
pub use system_message::{SystemMessage, UserReadSystemMessage};
pub use time_special::{ActiveTimeSpecial, TimeSpecial};
pub use tutorial::{
    TutorialSectionAvailableHtmlSymbol, TutorialSectionEntry, VisitedTutorialSectionEntry,
};
pub use unit::Unit;
pub use unit_type::UnitType;
pub use upgrade::{ObtainedUpgrade, Upgrade, UpgradeType};
pub use user_storage::UserStorage;
