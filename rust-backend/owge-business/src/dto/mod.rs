//! DTOs — the JSON payloads the frontend consumes, mirroring the Java
//! `com.kevinguanchedarias.owgejava.dto` classes. Field names are Jackson
//! camelCase, so every struct uses `#[serde(rename_all = "camelCase")]`.

pub mod alliance;
pub mod attack_rule;
pub mod serde_helpers;
pub mod configuration;
pub mod critical_attack;
pub mod critical_attack_information;
pub mod faction;
pub mod galaxy;
pub mod image_store;
pub mod improvement;
pub mod mission;
pub mod mission_report_information;
pub mod mission_websocket;
pub mod obtained_unit;
pub mod planet;
pub mod planet_list;
pub mod ranking;
pub mod requirement;
pub mod requirement_information;
pub mod rule;
pub mod running_unit_build;
pub mod special_location;
pub mod speed_impact_group;
pub mod system_message;
pub mod time_special;
pub mod translatable;
pub mod tutorial;
pub mod unit;
pub mod unit_type;
pub mod upgrade;
pub mod user;
pub mod user_improvement;
pub mod websocket;

pub use alliance::{AllianceDto, AllianceJoinRequestDto, JoinRequestIdBody, RequestJoinBody};
pub use attack_rule::{AttackRuleDto, AttackRuleEntryDto, AttackRuleEntryInput, AttackRuleInput};
pub use configuration::{ConfigurationDto, ConfigurationInput};
pub use critical_attack::{
    CriticalAttackDto, CriticalAttackEntryDto, CriticalAttackEntryInput, CriticalAttackInput,
};
pub use critical_attack_information::CriticalAttackInformationResponse;
pub use faction::{
    FactionDto, FactionInput, FactionSpawnLocationDto, FactionSpawnLocationInput,
    FactionUnitTypeDto, FactionUnitTypeOverrideInput,
};
pub use galaxy::{GalaxyDto, GalaxyInput};
pub use image_store::{ImageStoreDto, ImageStoreInput, ImageUploadInput};
pub use improvement::{ImprovementDto, ImprovementUnitTypeDto};
pub use mission::{GatherMissionResultDto, MissionDto, MissionReportDto, UnitRunningMissionDto};
pub use mission_report_information::MissionReportResponse;
pub use obtained_unit::{ObtainedUnitDto, ObtainedUnitUnitDto, TemporalInformationDto};
pub use planet::PlanetDto;
pub use planet_list::PlanetListDto;
pub use ranking::{RankingEntryDto, RankingFactionDto};
pub use requirement::{UnitUpgradeRequirement, UnitWithRequirementInfo};
pub use requirement_information::{
    ObjectRelationDto, RequirementCodeInput, RequirementDto, RequirementGroupDto,
    RequirementGroupInput, RequirementInformationDto, RequirementInformationInput,
};
pub use rule::{
    IdNameDto, RuleDto, RuleExtraArgDto, RuleInput, RuleItemTypeDescriptorDto,
    RuleTypeDescriptorDto, RuleWithRelatedUnitsDto, UnitCommonDto,
};
pub use running_unit_build::RunningUnitBuildDto;
pub use special_location::{SpecialLocationDto, SpecialLocationInput};
pub use speed_impact_group::{SpeedImpactGroupDto, SpeedImpactGroupInput};
pub use system_message::{SystemMessageDto, SystemMessageInput, SystemMessageUserDto};
pub use time_special::{ActiveTimeSpecialDto, TimeSpecialDto};
pub use translatable::{TranslatableInput, TranslatableTranslationInput};
pub use tutorial::{
    TranslatableDto, TranslatableTranslationDto, TutorialSectionAvailableHtmlSymbolDto,
    TutorialSectionDto, TutorialSectionEntryDto, TutorialSectionEntryInput,
};
pub use unit::{UnitDto, UnitInput};
pub use unit_type::{UnitTypeDto, UnitTypeInput};
pub use upgrade::{ObtainedUpgradeDto, UpgradeDto, UpgradeInput, UpgradeTypeDto, UpgradeTypeInput};
pub use user::{SimpleUserData, UserData};
pub use user_improvement::{
    GroupedImprovementResponse, ImprovementType, UnitTypeImprovementEntry, UserImprovementDto,
};
