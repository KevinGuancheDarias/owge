//! The resolve layer: loads the universe configuration **through the engine's
//! own `owge-business` bo functions** (so rule/critical/speed-group fallbacks
//! and the requirement-group OR semantics can never drift from the game) and
//! inverts the requirement graph both ways into the [`Site`] view model.

use std::collections::{BTreeMap, HashMap, HashSet};

use anyhow::Result;
use sqlx::MySqlConnection;

use owge_business::OwgeError;
use owge_business::bo::attack_mission_manager_bo::AttackMissionManagerBo;
use owge_business::bo::attack_rule_bo::AttackRuleBo;
use owge_business::bo::configuration_bo::ConfigurationBo;
use owge_business::bo::critical_attack_bo::CriticalAttackBo;
use owge_business::bo::faction_bo::FactionBo;
use owge_business::bo::galaxy_bo::GalaxyBo;
use owge_business::bo::improvement_bo::ImprovementBo;
use owge_business::bo::requirement_bo::RequirementBo;
use owge_business::bo::requirement_group_bo::RequirementGroupBo;
use owge_business::bo::rule_bo::RuleBo;
use owge_business::bo::special_location_bo::SpecialLocationBo;
use owge_business::bo::speed_impact_group_bo::SpeedImpactGroupBo;
use owge_business::bo::temporal_units_bo::TemporalUnitsBo;
use owge_business::bo::time_special_bo::TimeSpecialBo;
use owge_business::bo::unit_bo::UnitBo;
use owge_business::bo::unit_interception_finder_bo::UnitInterceptionFinderBo;
use owge_business::bo::unit_type_bo::UnitTypeBo;
use owge_business::bo::upgrade_bo::UpgradeBo;
use owge_business::dto::{FactionDto, GalaxyDto, ImprovementDto, RequirementInformationDto};
use owge_business::model::Configuration;
use owge_business::model::object_relation::object_enum;
use owge_business::model::unit::Unit as UnitRow;

use crate::i18n::Tr;
use crate::view::*;

/// Section keys — double as the inverse-index target kinds.
const UNITS: &str = "units";
const UPGRADES: &str = "upgrades";
const TIME_SPECIALS: &str = "time-specials";
const SPECIAL_LOCATIONS: &str = "special-locations";
const FACTIONS: &str = "factions";
const GALAXIES: &str = "galaxies";
const UNIT_TYPES: &str = "unit-types";
const TRAVEL_GROUPS: &str = "travel-groups";

/// Name/href lookups for every referenceable entity, built up-front.
#[derive(Default)]
struct Names {
    maps: HashMap<&'static str, HashMap<i64, Link>>,
}

impl Names {
    fn insert(&mut self, kind: &'static str, id: i64, href: String, label: String) {
        self.maps
            .entry(kind)
            .or_default()
            .insert(id, Link { href, label });
    }

    fn link(&self, kind: &str, id: i64) -> Option<Link> {
        self.maps.get(kind).and_then(|m| m.get(&id)).cloned()
    }

    fn label(&self, kind: &str, id: i64) -> String {
        self.link(kind, id)
            .map(|l| l.label)
            .unwrap_or_else(|| format!("(missing #{id})"))
    }
}

/// One raw requirement, kept for inverse-index building after rendering.
struct RawReq {
    code: String,
    second: Option<i64>,
    third: Option<i64>,
}

/// The inverse of the requirement graph: (target kind, target id) → the
/// content whose requirements reference it.
#[derive(Default)]
struct Inverse {
    unlocks: HashMap<(&'static str, i64), Vec<UnlockLine>>,
    /// UPGRADE_LEVEL_LOWER_THAN inverse, keyed by upgrade id: content that a
    /// high level of the upgrade *locks*.
    blocks: HashMap<i64, Vec<UnlockLine>>,
}

impl Inverse {
    fn register(
        &mut self,
        raw: &RawReq,
        kind_label: &str,
        content: Option<Link>,
        name: &str,
        tr: &'static Tr,
    ) {
        let Some(second) = raw.second else { return };
        let third = || raw.third.unwrap_or(0).to_string();
        let (target_kind, detail, is_block): (&'static str, String, bool) = match raw.code.as_str()
        {
            "HAVE_SPECIAL_LOCATION" => (SPECIAL_LOCATIONS, String::new(), false),
            "HAVE_UNIT" => (UNITS, String::new(), false),
            "UNIT_AMOUNT" => (
                UNITS,
                tr.inv_unit_amount.replace("{n}", &fmt_int(raw.third.unwrap_or(0))),
                false,
            ),
            "BEEN_RACE" => (FACTIONS, String::new(), false),
            "UPGRADE_LEVEL" => (UPGRADES, tr.inv_upgrade_level.replace("{n}", &third()), false),
            "HOME_GALAXY" => (GALAXIES, String::new(), false),
            "HAVE_SPECIAL_AVAILABLE" => (TIME_SPECIALS, tr.inv_special_available.into(), false),
            "HAVE_SPECIAL_ENABLED" => (TIME_SPECIALS, tr.inv_special_enabled.into(), false),
            "UPGRADE_LEVEL_LOWER_THAN" => {
                (UPGRADES, tr.inv_upgrade_lower.replace("{n}", &third()), true)
            }
            _ => return,
        };
        let line = UnlockLine {
            kind: kind_label.to_string(),
            link: content,
            label: name.to_string(),
            detail,
        };
        if is_block {
            self.blocks.entry(second).or_default().push(line);
        } else {
            self.unlocks
                .entry((target_kind, second))
                .or_default()
                .push(line);
        }
    }
}

/// Render one `requirements_information` row human-readably.
fn req_line(
    code: &str,
    second: Option<i64>,
    third: Option<i64>,
    names: &Names,
    tr: &'static Tr,
) -> ReqLine {
    let link = |kind: &str| second.and_then(|id| names.link(kind, id));
    let missing = |kind: &str| {
        second
            .filter(|id| names.link(kind, *id).is_none())
            .map(|id| tr.missing_ref.replace("{id}", &id.to_string()))
            .unwrap_or_default()
    };
    let third_str = third.unwrap_or(0).to_string();
    let (prefix, kind, suffix) = match code {
        "HAVE_SPECIAL_LOCATION" => (tr.req_special_location, SPECIAL_LOCATIONS, String::new()),
        "HAVE_UNIT" => (tr.req_have_unit, UNITS, String::new()),
        "UNIT_AMOUNT" => (
            tr.req_unit_amount_prefix,
            UNITS,
            tr.req_unit_amount_suffix
                .replace("{n}", &fmt_int(third.unwrap_or(0))),
        ),
        "BEEN_RACE" => (tr.req_been_race, FACTIONS, String::new()),
        "UPGRADE_LEVEL" => (
            tr.req_upgrade_prefix,
            UPGRADES,
            tr.req_upgrade_level_suffix.replace("{n}", &third_str),
        ),
        "UPGRADE_LEVEL_LOWER_THAN" => (
            tr.req_upgrade_prefix,
            UPGRADES,
            tr.req_upgrade_lower_suffix.replace("{n}", &third_str),
        ),
        "HOME_GALAXY" => (tr.req_home_galaxy, GALAXIES, String::new()),
        "HAVE_SPECIAL_AVAILABLE" => (
            tr.req_time_special_prefix,
            TIME_SPECIALS,
            tr.req_time_special_available_suffix.into(),
        ),
        "HAVE_SPECIAL_ENABLED" => (
            tr.req_time_special_prefix,
            TIME_SPECIALS,
            tr.req_time_special_enabled_suffix.into(),
        ),
        "WORST_PLAYER" => {
            return ReqLine {
                prefix: tr.req_worst_player.into(),
                link: None,
                suffix: String::new(),
            };
        }
        other => {
            return ReqLine {
                prefix: format!("{other} ({}, {})", second.unwrap_or(0), third.unwrap_or(0)),
                link: None,
                suffix: String::new(),
            };
        }
    };
    ReqLine {
        prefix: format!("{prefix}{}", missing(kind)),
        link: link(kind),
        suffix,
    }
}

fn raw_of(dto: &RequirementInformationDto) -> RawReq {
    RawReq {
        code: dto.requirement.code.clone(),
        second: dto.second_value,
        third: dto.third_value,
    }
}

/// The unlock view of one content entity, with the engine's exact semantics:
/// slave `REQUIREMENT_GROUP` relations are alternative (OR) paths, each AND
/// inside; without groups the relation's direct requirements are one AND set.
async fn req_groups_view(
    conn: &mut MySqlConnection,
    object: &str,
    reference_id: i16,
    names: &Names,
    tr: &'static Tr,
) -> Result<(Vec<ReqGroupView>, Vec<RawReq>)> {
    let mut raws = Vec::new();
    let groups = RequirementGroupBo::find_groups(&mut *conn, object, reference_id).await?;
    if !groups.is_empty() {
        let views = groups
            .into_iter()
            .map(|g| {
                let reqs = g
                    .requirements
                    .iter()
                    .map(|r| {
                        raws.push(raw_of(r));
                        req_line(&r.requirement.code, r.second_value, r.third_value, names, tr)
                    })
                    .collect();
                ReqGroupView {
                    name: g.name.unwrap_or_default(),
                    reqs,
                }
            })
            .collect();
        return Ok((views, raws));
    }
    let direct = RequirementBo::find_requirements(&mut *conn, object, reference_id).await?;
    if direct.is_empty() {
        return Ok((Vec::new(), raws));
    }
    let reqs = direct
        .iter()
        .map(|r| {
            raws.push(raw_of(r));
            req_line(&r.requirement.code, r.second_value, r.third_value, names, tr)
        })
        .collect();
    Ok((
        vec![ReqGroupView {
            name: String::new(),
            reqs,
        }],
        raws,
    ))
}

/// Improvement lines for an entity, `[]` when it has none (`NotFound` from the
/// bo means "no improvement linked" — same signal the engine gets).
async fn improvement_lines(
    conn: &mut MySqlConnection,
    table: &str,
    id: u16,
    names: &Names,
    tr: &'static Tr,
) -> Result<Vec<String>> {
    match ImprovementBo::find_for_entity_shallow(&mut *conn, table, id).await {
        Ok(dto) => Ok(render_improvement(&dto, names, tr)),
        Err(OwgeError::NotFound(_)) => Ok(Vec::new()),
        Err(e) => Err(e.into()),
    }
}

fn render_improvement(dto: &ImprovementDto, names: &Names, tr: &'static Tr) -> Vec<String> {
    let mut out = Vec::new();
    let mut pct = |label: &str, v: Option<f32>| {
        if let Some(v) = v
            && v != 0.0 {
                out.push(format!("{label} +{}%", fmt_f32(v)));
            }
    };
    pct(tr.imp_primary, dto.more_primary_resource_production);
    pct(tr.imp_secondary, dto.more_secondary_resource_production);
    pct(tr.imp_energy, dto.more_energy_production);
    pct(tr.imp_charge, dto.more_charge_capacity);
    pct(tr.imp_missions, dto.more_missions);
    pct(tr.imp_research_speed, dto.more_upgrade_research_speed);
    pct(tr.imp_build_speed, dto.more_unit_build_speed);
    for e in &dto.unit_types_upgrades {
        let value = match e.value {
            Some(v) if v != 0 => v,
            _ => continue,
        };
        let label = match e.r#type.as_deref() {
            Some("ATTACK") => tr.imp_type_attack,
            Some("DEFENSE") => tr.imp_type_defense,
            Some("SHIELD") => tr.imp_type_shield,
            Some("AMOUNT") => tr.imp_type_amount,
            Some("SPEED") => tr.imp_type_speed,
            _ => tr.imp_type_generic,
        };
        let type_name = e
            .unit_type_name
            .clone()
            .or_else(|| e.resolved_unit_type_id().map(|id| names.label(UNIT_TYPES, id as i64)))
            .unwrap_or_else(|| "?".into());
        out.push(format!(
            "{label} +{}%{}{type_name}",
            fmt_int(value),
            tr.imp_for_unit_type
        ));
    }
    out
}

/// The faction ids a set of raw requirements gates on (BEEN_RACE, across
/// direct requirements and every OR-group alike).
fn required_factions(raws: &[RawReq]) -> Vec<i64> {
    let mut out: Vec<i64> = raws
        .iter()
        .filter(|r| r.code == "BEEN_RACE")
        .filter_map(|r| r.second)
        .collect();
    out.sort_unstable();
    out.dedup();
    out
}

/// "Unlocked by default" = nothing beyond faction membership and upgrade
/// levels gates it; anything else (special locations, units, home galaxy…)
/// means extra steps.
fn is_default_unlock(raws: &[RawReq]) -> bool {
    raws.iter()
        .all(|r| r.code == "BEEN_RACE" || r.code == "UPGRADE_LEVEL")
}

/// Detects the per-faction clone workaround in an unlock list: requirement OR
/// groups are only supported for travel groups, so to offer one hero/special
/// to several factions admins create it once per faction ("Foo (Wraith)",
/// "Foo (Ori)"…). Two labels count as clones when they collide after removing
/// all spaces (check 1), or after additionally removing a "(FactionName)"
/// fragment (check 2).
fn has_cloned_unlocks(lines: &[UnlockLine], faction_names: &[String]) -> bool {
    let nospace = |s: &str| s.chars().filter(|c| !c.is_whitespace()).collect::<String>();
    let faction_parens: Vec<String> = faction_names
        .iter()
        .map(|f| format!("({})", nospace(f)))
        .collect();
    let mut seen = HashSet::new();
    let mut seen_defactioned = HashSet::new();
    for line in lines {
        let name = nospace(&line.label);
        let mut defactioned = name.clone();
        for paren in &faction_parens {
            defactioned = defactioned.replace(paren.as_str(), "");
        }
        if !seen.insert(name) || !seen_defactioned.insert(defactioned) {
            return true;
        }
    }
    false
}

fn list_row(p: &EntityPage) -> ListRow {
    ListRow {
        href: p.href.clone(),
        image_url: p.image_url.clone(),
        name: p.name.clone(),
        subtitle: p.subtitle.clone(),
        badges: p.badges.clone(),
    }
}

/// Group a sorted page list by required faction, "Any faction" last. Pages
/// unlockable by several factions (alternative OR paths) appear under each.
/// When `default_of` is given, each group is split into "Unlocked by default"
/// / "Requires further unlocks" subgroups.
fn faction_groups(
    pages: &[EntityPage],
    factions_of: &HashMap<u16, Vec<i64>>,
    default_of: Option<&HashMap<u16, bool>>,
    factions: &[FactionDto],
    names: &Names,
    tr: &'static Tr,
) -> Vec<ListGroup> {
    let bucket = |members: Vec<&EntityPage>, title: String, link: Option<Link>| {
        let Some(default_of) = default_of else {
            return ListGroup {
                title,
                link,
                rows: members.iter().map(|p| list_row(p)).collect(),
                subgroups: Vec::new(),
            };
        };
        let (default, further): (Vec<&&EntityPage>, Vec<&&EntityPage>) = members
            .iter()
            .partition(|p| default_of.get(&p.id).copied().unwrap_or(true));
        let mut subgroups = Vec::new();
        if !default.is_empty() {
            subgroups.push(ListSubgroup {
                title: tr.unlocked_by_default.into(),
                rows: default.iter().map(|p| list_row(p)).collect(),
            });
        }
        if !further.is_empty() {
            subgroups.push(ListSubgroup {
                title: tr.requires_further.into(),
                rows: further.iter().map(|p| list_row(p)).collect(),
            });
        }
        ListGroup {
            title,
            link,
            rows: Vec::new(),
            subgroups,
        }
    };

    let mut out = Vec::new();
    for f in factions {
        let members: Vec<&EntityPage> = pages
            .iter()
            .filter(|p| {
                factions_of
                    .get(&p.id)
                    .is_some_and(|v| v.contains(&(f.id as i64)))
            })
            .collect();
        if !members.is_empty() {
            out.push(bucket(
                members,
                f.name.clone(),
                names.link(FACTIONS, f.id as i64),
            ));
        }
    }
    let members: Vec<&EntityPage> = pages
        .iter()
        .filter(|p| factions_of.get(&p.id).is_none_or(|v| v.is_empty()))
        .collect();
    if !members.is_empty() {
        out.push(bucket(members, tr.any_faction.into(), None));
    }
    out
}

/// Group a sorted special-location page list by galaxy, in galaxy display
/// order, with an "Unassigned" bucket last for locations not tied to a galaxy.
/// A location's galaxy is coarse enough to always reveal; only its exact
/// planet (the "found at" coordinates) is gated by `WIKI_EXPOSE_SPECIAL_LOCATIONS`.
fn galaxy_groups(
    pages: &[EntityPage],
    galaxy_of: &HashMap<u16, Option<u16>>,
    galaxies: &[GalaxyDto],
    names: &Names,
    tr: &'static Tr,
) -> Vec<ListGroup> {
    let flat = |title: String, link: Option<Link>, members: Vec<&EntityPage>| ListGroup {
        title,
        link,
        rows: members.iter().map(|p| list_row(p)).collect(),
        subgroups: Vec::new(),
    };
    let mut out = Vec::new();
    for g in galaxies {
        let members: Vec<&EntityPage> = pages
            .iter()
            .filter(|p| galaxy_of.get(&p.id).copied().flatten() == Some(g.id))
            .collect();
        if !members.is_empty() {
            out.push(flat(
                format!("{}{}", tr.galaxy_prefix, g.name),
                names.link(GALAXIES, g.id as i64),
                members,
            ));
        }
    }
    let unassigned: Vec<&EntityPage> = pages
        .iter()
        .filter(|p| galaxy_of.get(&p.id).copied().flatten().is_none())
        .collect();
    if !unassigned.is_empty() {
        out.push(flat(tr.st_unassigned.into(), None, unassigned));
    }
    out
}

fn mission_support(v: &str, tr: &'static Tr) -> Option<&'static str> {
    match v {
        "NONE" => Some(tr.mi_never),
        "OWNED_ONLY" => Some(tr.mi_owned_only),
        _ => None, // ANY: not worth a row
    }
}

fn mission_limit_rows(pairs: &[(&'static str, &str)], tr: &'static Tr) -> Vec<Kv> {
    pairs
        .iter()
        .filter_map(|(mission, v)| {
            mission_support(v, tr).map(|txt| Kv {
                k: mission.to_string(),
                v: txt.to_string(),
            })
        })
        .collect()
}

/// The mission types that have tunable time/speed configuration params
/// (matches the engine's `MissionType` values used by `MissionTimeManagerBo`).
const CONFIG_MISSION_TYPES: [(&str, fn(&Tr) -> &'static str); 7] = [
    ("EXPLORE", |tr| tr.mi_explore),
    ("GATHER", |tr| tr.mi_gather),
    ("ESTABLISH_BASE", |tr| tr.mi_establish_base),
    ("ATTACK", |tr| tr.mi_attack),
    ("COUNTERATTACK", |tr| tr.mi_counterattack),
    ("CONQUEST", |tr| tr.mi_conquest),
    ("DEPLOY", |tr| tr.mi_deploy),
];

/// Integer config values get thousands separators; anything else (floats,
/// enums, free text) is shown as stored.
fn fmt_config_num(v: &str) -> String {
    let v = v.trim();
    match v.parse::<i64>() {
        Ok(n) => fmt_int(n),
        Err(_) => v
            .parse::<f32>()
            .map(fmt_f32)
            .unwrap_or_else(|_| v.to_string()),
    }
}

/// The universe-configuration page. Only **non-privileged** rows are read
/// (the same predicate as the game's public configuration endpoint), so
/// secrets can never leak into the generated site. Known gameplay params are
/// rendered as human sentences; whatever is left lands raw in "Other".
async fn config_sections(
    conn: &mut MySqlConnection,
    tr: &'static Tr,
) -> Result<Vec<ConfigSection>> {
    let mut rows: BTreeMap<String, Configuration> =
        ConfigurationBo::find_all_non_privileged(&mut *conn)
            .await?
            .into_iter()
            .map(|c| (c.name.clone(), c))
            .collect();
    let raw_kv = |c: &Configuration| Kv {
        k: c.name.clone(),
        v: c.value.clone(),
    };
    let yes_no = |v: &str| {
        if v.eq_ignore_ascii_case("TRUE") {
            tr.cfg_yes
        } else {
            tr.cfg_no
        }
    };
    let entry = |c: &Configuration, label: &str, value: String| ConfigEntry {
        label: label.to_string(),
        value,
        raws: vec![raw_kv(c)],
    };

    let mut general = Vec::new();
    if let Some(c) = rows.remove("DEPLOYMENT_CONFIG") {
        let value = match c.value.as_str() {
            "FREEDOM" => tr.cfg_deploy_freedom.into(),
            "ONLY_ONCE_RETURN_DEPLOYED" => tr.cfg_deploy_return_deployed.into(),
            "ONLY_ONCE_RETURN_SOURCE" => tr.cfg_deploy_return_source.into(),
            "DISALLOWED" => tr.cfg_disabled.into(),
            other => other.to_string(),
        };
        general.push(entry(&c, tr.cfg_deploy, value));
    }
    if let Some(c) = rows.remove("ZERO_BUILD_TIME") {
        general.push(entry(&c, tr.cfg_zero_build, yes_no(&c.value).into()));
    }
    if let Some(c) = rows.remove("ZERO_UPGRADE_TIME") {
        general.push(entry(&c, tr.cfg_zero_upgrade, yes_no(&c.value).into()));
    }
    if let Some(c) = rows.remove("IMPROVEMENT_STEP") {
        general.push(entry(
            &c,
            tr.cfg_improvement_step,
            format!("{}%", fmt_config_num(&c.value)),
        ));
    }
    if let Some(c) = rows.remove("DISABLED_FEATURE_ALLIANCE") {
        // The param disables the feature, so TRUE reads as "Disabled".
        let value = if c.value.eq_ignore_ascii_case("TRUE") {
            tr.cfg_disabled
        } else {
            tr.cfg_enabled
        };
        general.push(entry(&c, tr.cfg_alliances, value.into()));
    }
    if let Some(c) = rows.remove("ALLIANCE_MAX_SIZE") {
        general.push(entry(&c, tr.cfg_alliance_max_size, fmt_config_num(&c.value)));
    }
    if let Some(c) = rows.remove("ALLIANCE_MAX_SIZE_PERCENTAGE") {
        general.push(entry(
            &c,
            tr.cfg_alliance_max_pct,
            format!("{}%", fmt_config_num(&c.value)),
        ));
    }
    if let Some(c) = rows.remove("WIKI_EXPOSE_SPECIAL_LOCATIONS") {
        general.push(entry(&c, tr.cfg_wiki_expose, yes_no(&c.value).into()));
    }
    if let Some(c) = rows.remove("ATTACK_DETERMINISTIC_RNG") {
        general.push(entry(&c, tr.cfg_deterministic_rng, yes_no(&c.value).into()));
    }

    let mut sections = Vec::new();
    if !general.is_empty() {
        sections.push(ConfigSection {
            slug: "general".into(),
            title: tr.cfg_general.into(),
            entries: general,
        });
    }

    for (key, label_of) in CONFIG_MISSION_TYPES {
        let mut entries = Vec::new();
        if let Some(c) = rows.remove(&format!("MISSION_TIME_{key}")) {
            let value = c
                .value
                .trim()
                .parse::<i64>()
                .map(fmt_secs)
                .unwrap_or_else(|_| c.value.clone());
            entries.push(entry(&c, tr.cfg_base_time, value));
        }
        // Multi-param entries: each present piece becomes "label: value" and
        // contributes its raw row to the shared details block.
        let multi = |suffixes: &[(&str, &'static str)],
                         rows: &mut BTreeMap<String, Configuration>| {
            let mut parts = Vec::new();
            let mut raws = Vec::new();
            for (suffix, part_label) in suffixes {
                if let Some(c) = rows.remove(&format!("MISSION_SPEED_{key}_{suffix}")) {
                    parts.push(format!("{part_label}: {}", fmt_config_num(&c.value)));
                    raws.push(raw_kv(&c));
                }
            }
            (parts, raws)
        };
        let (parts, raws) = multi(
            &[
                ("SAME_Q", tr.cfg_same_q),
                ("DIFF_Q", tr.cfg_diff_q),
                ("DIFF_S", tr.cfg_diff_s),
                ("DIFF_G", tr.cfg_diff_g),
            ],
            &mut rows,
        );
        if !parts.is_empty() {
            entries.push(ConfigEntry {
                label: tr.cfg_distance_penalty.into(),
                value: parts.join(" · "),
                raws,
            });
        }
        let (parts, raws) = multi(
            &[
                ("P_MOVE_COST", tr.cfg_per_planet),
                ("Q_MOVE_COST", tr.cfg_per_quadrant),
                ("S_MOVE_COST", tr.cfg_per_sector),
                ("G_MOVE_COST", tr.cfg_cross_galaxy),
            ],
            &mut rows,
        );
        if !parts.is_empty() {
            entries.push(ConfigEntry {
                label: tr.cfg_move_cost.into(),
                value: parts.join(" · "),
                raws,
            });
        }
        if let Some(c) = rows.remove(&format!("MISSION_SPEED_DIVISOR_{key}")) {
            entries.push(entry(&c, tr.cfg_divisor, fmt_config_num(&c.value)));
        }
        if let Some(c) = rows.remove(&format!("MISSION_{key}_TRIGGER_ATTACK")) {
            entries.push(entry(&c, tr.cfg_trigger_attack, yes_no(&c.value).into()));
        }
        if !entries.is_empty() {
            sections.push(ConfigSection {
                slug: key.to_lowercase(),
                title: tr.cfg_mission_section.replace("{m}", label_of(tr)),
                entries,
            });
        }
    }

    // Anything the wiki doesn't know how to phrase: the admin's display name
    // (when set) plus the stored value, still raw-inspectable.
    if !rows.is_empty() {
        let entries = rows
            .values()
            .map(|c| ConfigEntry {
                label: c
                    .display_name
                    .clone()
                    .filter(|d| !d.trim().is_empty())
                    .unwrap_or_else(|| c.name.clone()),
                value: c.value.clone(),
                raws: vec![raw_kv(c)],
            })
            .collect();
        sections.push(ConfigSection {
            slug: "other".into(),
            title: tr.cfg_other.into(),
            entries,
        });
    }
    Ok(sections)
}

/// Build the resolved site once per configured language (the DB content is
/// identical; only the chrome strings differ).
pub async fn build_all_languages(
    conn: &mut MySqlConnection,
    universe: &str,
) -> Result<Vec<(&'static crate::i18n::LangDef, Site)>> {
    let mut out = Vec::with_capacity(crate::i18n::LANGS.len());
    for lang in &crate::i18n::LANGS {
        out.push((lang, build_site(&mut *conn, universe, lang.tr).await?));
    }
    Ok(out)
}

pub async fn build_site(
    conn: &mut MySqlConnection,
    universe: &str,
    tr: &'static Tr,
) -> Result<Site> {
    // ---- Load every catalog through the engine's own read paths. ----
    let units = UnitBo::find_all(&mut *conn).await?;
    let unit_rows: Vec<UnitRow> = sqlx::query_as("SELECT * FROM units")
        .fetch_all(&mut *conn)
        .await?;
    let unit_row_by_id: HashMap<u16, &UnitRow> = unit_rows.iter().map(|u| (u.id, u)).collect();
    let unit_types = UnitTypeBo::find_all(&mut *conn).await?;
    let upgrades = UpgradeBo::find_all(&mut *conn).await?;
    // user 0 exists for no one → no active-time-special decoration.
    let time_specials = TimeSpecialBo::find_all_dtos(&mut *conn, 0).await?;
    let special_locations = SpecialLocationBo::find_all(&mut *conn).await?;
    // WIKI_EXPOSE_SPECIAL_LOCATIONS (TRUE/FALSE, absent = FALSE): whether the
    // wiki may reveal each special location's *exact* planet. The galaxy is
    // coarse enough to always show (grouping, subtitle, galaxies page); this
    // flag only gates the precise "found at" planet coordinates.
    let expose_special_locations =
        ConfigurationBo::find_opt(&mut *conn, "WIKI_EXPOSE_SPECIAL_LOCATIONS")
            .await?
            .is_some_and(|c| c.value.eq_ignore_ascii_case("TRUE"));
    let factions = FactionBo::find_all(&mut *conn).await?;
    let galaxies = GalaxyBo::find_all(&mut *conn).await?;
    let travel_groups = SpeedImpactGroupBo::find_all_dtos(&mut *conn).await?;

    // ---- Name/href lookups. ----
    let mut names = Names::default();
    for u in &units {
        names.insert(UNITS, u.id as i64, format!("units/{}.html", u.id), u.name.clone());
    }
    for u in &upgrades {
        names.insert(UPGRADES, u.id as i64, format!("upgrades/{}.html", u.id), u.name.clone());
    }
    for t in &time_specials {
        names.insert(
            TIME_SPECIALS,
            t.id as i64,
            format!("time-specials/{}.html", t.id),
            t.name.clone(),
        );
    }
    for s in &special_locations {
        names.insert(
            SPECIAL_LOCATIONS,
            s.id as i64,
            format!("special-locations/{}.html", s.id),
            s.name.clone(),
        );
    }
    for f in &factions {
        names.insert(FACTIONS, f.id as i64, format!("factions/{}.html", f.id), f.name.clone());
    }
    for g in &galaxies {
        names.insert(GALAXIES, g.id as i64, format!("galaxies.html#g{}", g.id), g.name.clone());
    }
    for t in &unit_types {
        names.insert(UNIT_TYPES, t.id as i64, format!("unit-types.html#t{}", t.id), t.name.clone());
    }
    for s in &travel_groups {
        names.insert(
            TRAVEL_GROUPS,
            s.id as i64,
            format!("travel-groups.html#s{}", s.id),
            s.name.clone(),
        );
    }

    let mut inverse = Inverse::default();

    // For the attackable-types panel: parent chain per unit type, and the
    // types the game shows (its panel filters on the engine's `used` flag).
    let ut_parent: HashMap<u16, Option<u16>> = unit_types
        .iter()
        .map(|t| (t.id, t.parent.as_ref().map(|p| p.id)))
        .collect();
    let mut used_type_ids: Vec<u16> = Vec::new();
    for t in &unit_types {
        if UnitTypeBo::is_used(&mut *conn, t.id).await? {
            used_type_ids.push(t.id);
        }
    }

    // ---- Units. ----
    let mut unit_pages = Vec::with_capacity(units.len());
    let mut unit_factions: HashMap<u16, Vec<i64>> = HashMap::new();
    let mut unit_defaults: HashMap<u16, bool> = HashMap::new();
    // (unit id, effective travel group, interceptable groups) for the
    // interception matrix second pass.
    let mut interception: Vec<(u16, Option<u16>, Vec<u16>)> = Vec::with_capacity(units.len());
    for dto in &units {
        let mut page = EntityPage::new(dto.id, UNITS, dto.name.clone());
        page.image_url = dto.image_url.clone();
        page.description = dto.description.clone().unwrap_or_default();
        page.header_link = dto
            .type_id
            .and_then(|tid| names.link(UNIT_TYPES, tid as i64));
        page.subtitle = dto.type_name.clone().unwrap_or_default();
        if dto.is_unique {
            page.badges.push(tr.badge_unique.into());
        }
        if dto.bypass_shield {
            page.badges.push(tr.badge_bypass_shield.into());
        }
        if dto.is_invisible {
            page.badges.push(tr.badge_invisible.into());
        }
        if dto.can_fast_explore {
            page.badges.push(tr.badge_fast_explorer.into());
        }
        let mut stat = |k: &str, v: String| {
            page.stats.push(Kv {
                k: k.into(),
                v,
            })
        };
        if let Some(v) = dto.points {
            stat(tr.st_points, fmt_int(v as i64));
        }
        if let Some(v) = dto.time {
            stat(tr.st_build_time, fmt_secs(v as i64));
        }
        if dto.primary_resource.is_some() || dto.secondary_resource.is_some() {
            stat(
                tr.st_cost,
                tr.st_cost_fmt
                    .replace("{p}", &fmt_int(dto.primary_resource.unwrap_or(0) as i64))
                    .replace("{s}", &fmt_int(dto.secondary_resource.unwrap_or(0) as i64)),
            );
        }
        if let Some(v) = dto.energy
            && v != 0 {
                stat(tr.st_energy, fmt_int(v as i64));
            }
        if let Some(v) = dto.attack {
            stat(tr.st_attack, fmt_int(v as i64));
        }
        if let Some(v) = dto.health {
            stat(tr.st_health, fmt_int(v as i64));
        }
        if let Some(v) = dto.shield
            && v != 0 {
                stat(tr.st_shield, fmt_int(v as i64));
            }
        if let Some(v) = dto.charge
            && v != 0 {
                stat(tr.st_charge, fmt_int(v as i64));
            }
        if let Some(v) = dto.speed
            && v != 0.0 {
                stat(tr.st_speed, fmt_num(v));
            }
        if let Some(v) = dto.storage_capacity {
            stat(tr.st_storage, fmt_int(v as i64));
        }
        if dto.stored_weight != 1 {
            stat(tr.st_weight, fmt_int(dto.stored_weight as i64));
        }

        let (groups, raws) = req_groups_view(&mut *conn, "UNIT", dto.id as i16, &names, tr).await?;
        page.req_groups = groups;
        unit_factions.insert(dto.id, required_factions(&raws));
        unit_defaults.insert(dto.id, is_default_unlock(&raws));
        let self_link = names.link(UNITS, dto.id as i64);
        for raw in &raws {
            inverse.register(raw, tr.kind_unit, self_link.clone(), &dto.name, tr);
        }
        page.improvement = improvement_lines(&mut *conn, "units", dto.id, &names, tr).await?;

        // Combat: reuse the engine's exact fallback resolution.
        let row = unit_row_by_id.get(&dto.id);
        let own_rule = row.and_then(|r| r.attack_rule_id);
        let effective_rule = match own_rule {
            Some(id) => Some((id, tr.source_own)),
            None => AttackMissionManagerBo::find_attack_rule(&mut *conn, dto.type_id)
                .await?
                .map(|id| (id, tr.source_inherited)),
        };
        let mut combat = CombatView::default();
        if let Some((rule_id, source)) = effective_rule
            && let Some(rule) = AttackRuleBo::find_by_id(&mut *conn, rule_id).await? {
                combat.attack_rule = Some(RuleView {
                    name: rule.name.clone(),
                    source: source.into(),
                    entries: rule
                        .entries
                        .iter()
                        .map(|e| {
                            let kind = if e.target == "UNIT" { UNITS } else { UNIT_TYPES };
                            let label = if e.target == "UNIT" {
                                tr.target_unit_prefix
                            } else {
                                tr.target_unit_type_prefix
                            };
                            let name = e
                                .reference_name
                                .clone()
                                .unwrap_or_else(|| names.label(kind, e.reference_id as i64));
                            RuleEntryView {
                                can: e.can_attack,
                                target: format!("{label}{name}"),
                            }
                        })
                        .collect(),
                });
                // The game's "Attackable types" panel, frontend-computed there
                // (`UnitTypeService.canAttack`): for each used type, the first
                // entry whose UNIT_TYPE reference matches the type or one of
                // its ancestors decides; UNIT entries are skipped and no match
                // means attackable.
                combat.attackable_types = used_type_ids
                    .iter()
                    .filter_map(|type_id| {
                        let can = rule
                            .entries
                            .iter()
                            .find_map(|e| {
                                if e.target != "UNIT_TYPE" {
                                    return None;
                                }
                                let mut current = Some(*type_id);
                                while let Some(t) = current {
                                    if t as i64 == e.reference_id as i64 {
                                        return Some(e.can_attack);
                                    }
                                    current = ut_parent.get(&t).copied().flatten();
                                }
                                None
                            })
                            .unwrap_or(true);
                        names
                            .link(UNIT_TYPES, *type_id as i64)
                            .map(|link| AttackableChip { link, can })
                    })
                    .collect();
            }
        if let Some(crit_id) = UnitBo::find_used_critical_attack(&mut *conn, dto.id).await?
            && let Some(crit) = CriticalAttackBo::find_by_id(&mut *conn, crit_id).await? {
                let source = if row.and_then(|r| r.critical_attack_id).is_some() {
                    tr.source_own
                } else {
                    tr.source_inherited
                };
                let mut crit_entries: Vec<_> = crit.entries.iter().collect();
                crit_entries.sort_by(|a, b| b.value.total_cmp(&a.value));
                combat.critical = Some(CritView {
                    name: crit.name.clone(),
                    source: source.into(),
                    entries: crit_entries
                        .into_iter()
                        .map(|e| {
                            let kind = if e.target == "UNIT" { UNITS } else { UNIT_TYPES };
                            let label = if e.target == "UNIT" {
                                tr.target_unit_prefix
                            } else {
                                tr.target_unit_type_prefix
                            };
                            let name = e
                                .reference_name
                                .clone()
                                .unwrap_or_else(|| names.label(kind, e.reference_id as i64));
                            CritEntryView {
                                target: format!("{label}{name}"),
                                value: format!("×{}", fmt_f32(e.value)),
                            }
                        })
                        .collect(),
                });
            }
        let effective_sig =
            UnitInterceptionFinderBo::find_his_or_inherited_speed_impact_group(&mut *conn, dto.id)
                .await?;
        combat.speed_group = effective_sig.and_then(|id| names.link(TRAVEL_GROUPS, id as i64));
        let intercepts =
            UnitInterceptionFinderBo::find_interceptable_group_ids(&mut *conn, dto.id).await?;
        combat.intercepts = intercepts
            .iter()
            .filter_map(|id| names.link(TRAVEL_GROUPS, *id as i64))
            .collect();
        interception.push((dto.id, effective_sig, intercepts));
        page.combat = Some(combat);
        unit_pages.push(page);
    }

    // Interception matrix inverse: who can intercept me = every unit whose
    // interceptable list contains my effective travel group.
    let unit_sig: HashMap<u16, Option<u16>> =
        interception.iter().map(|(id, sig, _)| (*id, *sig)).collect();
    for page in &mut unit_pages {
        let Some(Some(my_sig)) = unit_sig.get(&page.id) else {
            continue;
        };
        let hunters: Vec<Link> = interception
            .iter()
            .filter(|(other, _, intercepts)| *other != page.id && intercepts.contains(my_sig))
            .filter_map(|(other, _, _)| names.link(UNITS, *other as i64))
            .collect();
        if let Some(combat) = &mut page.combat {
            combat.intercepted_by = hunters;
        }
    }

    // ---- Upgrades. ----
    let mut upgrade_pages = Vec::with_capacity(upgrades.len());
    let mut upgrade_factions: HashMap<u16, Vec<i64>> = HashMap::new();
    for dto in &upgrades {
        let mut page = EntityPage::new(dto.id, UPGRADES, dto.name.clone());
        page.image_url = dto.image_url.clone();
        page.description = dto.description.clone().unwrap_or_default();
        page.subtitle = dto.type_name.clone().unwrap_or_default();
        if dto.type_id.is_none() {
            page.badges.push(tr.badge_hidden_no_type.into());
        }
        page.stats.push(Kv {
            k: tr.st_points.into(),
            v: fmt_int(dto.points as i64),
        });
        page.stats.push(Kv {
            k: tr.st_research_time.into(),
            v: fmt_secs(dto.time),
        });
        page.stats.push(Kv {
            k: tr.st_base_cost.into(),
            v: tr
                .st_cost_fmt
                .replace("{p}", &fmt_int(dto.primary_resource as i64))
                .replace("{s}", &fmt_int(dto.secondary_resource as i64)),
        });
        page.stats.push(Kv {
            k: tr.st_level_effect.into(),
            v: format!("+{}%", fmt_f32(dto.level_effect)),
        });
        let (groups, raws) =
            req_groups_view(&mut *conn, "UPGRADE", dto.id as i16, &names, tr).await?;
        page.req_groups = groups;
        upgrade_factions.insert(dto.id, required_factions(&raws));
        let self_link = names.link(UPGRADES, dto.id as i64);
        for raw in &raws {
            inverse.register(raw, tr.kind_upgrade, self_link.clone(), &dto.name, tr);
        }
        page.improvement = improvement_lines(&mut *conn, "upgrades", dto.id, &names, tr).await?;
        page.improvement_note = tr.note_per_level.into();
        upgrade_pages.push(page);
    }

    // ---- Time specials. ----
    let mut ts_pages = Vec::with_capacity(time_specials.len());
    let mut ts_factions: HashMap<u16, Vec<i64>> = HashMap::new();
    let mut ts_defaults: HashMap<u16, bool> = HashMap::new();
    // unit id → "provided temporarily by <time special>" lines, attached to
    // the unit pages after this loop.
    let mut temporal_sources: HashMap<i64, Vec<ReqLine>> = HashMap::new();
    for dto in &time_specials {
        let mut page = EntityPage::new(dto.id, TIME_SPECIALS, dto.name.clone());
        page.image_url = dto.image_url.clone();
        page.description = dto.description.clone().unwrap_or_default();
        page.subtitle = tr
            .ts_subtitle
            .replace("{d}", &fmt_secs(dto.duration as i64))
            .replace("{r}", &fmt_secs(dto.recharge_time as i64));
        page.stats.push(Kv {
            k: tr.st_duration.into(),
            v: fmt_secs(dto.duration as i64),
        });
        page.stats.push(Kv {
            k: tr.st_recharge.into(),
            v: fmt_secs(dto.recharge_time as i64),
        });
        let (groups, raws) =
            req_groups_view(&mut *conn, "TIME_SPECIAL", dto.id as i16, &names, tr).await?;
        page.req_groups = groups;
        ts_factions.insert(dto.id, required_factions(&raws));
        ts_defaults.insert(dto.id, is_default_unlock(&raws));
        let self_link = names.link(TIME_SPECIALS, dto.id as i64);
        for raw in &raws {
            inverse.register(raw, tr.kind_time_special, self_link.clone(), &dto.name, tr);
        }
        page.improvement =
            improvement_lines(&mut *conn, "time_specials", dto.id, &names, tr).await?;
        page.improvement_note = tr.note_while_active.into();
        // Units granted while active, with the engine's activation-time filter
        // (two extra args = duration#count, destination must be a UNIT; rows
        // pointing at deleted units are skipped like the listener does).
        for rule in TemporalUnitsBo::find_temporal_unit_rules(&mut *conn, dto.id).await? {
            if rule.extra_args.len() != 2 || rule.destination_type != object_enum::UNIT {
                continue;
            }
            let (Ok(duration), Ok(count)) = (
                rule.extra_args[0].parse::<i64>(),
                rule.extra_args[1].parse::<i64>(),
            ) else {
                continue;
            };
            let Some(unit_link) = names.link(UNITS, rule.destination_id) else {
                continue;
            };
            page.effects.push(EffectLine {
                prefix: format!("{} × ", fmt_int(count)),
                link: Some(unit_link),
                mid: String::new(),
                link2: None,
                suffix: tr
                    .temporal_grant_suffix
                    .replace("{d}", &fmt_secs(duration)),
            });
            temporal_sources
                .entry(rule.destination_id)
                .or_default()
                .push(ReqLine {
                    prefix: String::new(),
                    link: self_link.clone(),
                    suffix: tr
                        .temporal_source_suffix
                        .replace("{c}", &fmt_int(count))
                        .replace("{d}", &fmt_secs(duration)),
                });
        }
        // Hide-units and travel-group-swap rules. A UNIT destination matches
        // that exact unit; a UNIT_TYPE destination also covers its subtypes
        // (`RuleBo.isWantedUnitDestination` walks `parent_type`). Rules whose
        // target — or, for swaps, whose speed group — no longer exists are
        // dead at runtime too (`SpeedImpactGroupFinderBo.ruleToEntity`), skip.
        for rule in
            RuleBo::find_by_origin_type_and_origin_id(&mut *conn, "TIME_SPECIAL", dto.id as i64)
                .await?
        {
            let target = match rule.destination_type.as_str() {
                "UNIT" => names.link(UNITS, rule.destination_id),
                "UNIT_TYPE" => names.link(UNIT_TYPES, rule.destination_id),
                _ => None,
            };
            let Some(target) = target else { continue };
            match rule.r#type.as_str() {
                "TIME_SPECIAL_IS_ENABLED_DO_HIDE" => {
                    page.effects.push(EffectLine {
                        prefix: tr.effect_hide_prefix.into(),
                        link: Some(target),
                        mid: String::new(),
                        link2: None,
                        suffix: tr.effect_hide_suffix.into(),
                    });
                }
                "TIME_SPECIAL_IS_ENABLED_DO_SWAP_SPEED_IMPACT_GROUP" => {
                    let Some(group) = rule
                        .extra_args
                        .first()
                        .and_then(|arg| arg.parse::<i64>().ok())
                        .and_then(|id| names.link(TRAVEL_GROUPS, id))
                    else {
                        continue;
                    };
                    page.effects.push(EffectLine {
                        prefix: tr.effect_swap_prefix.into(),
                        link: Some(target),
                        mid: tr.effect_swap_mid.into(),
                        link2: Some(group),
                        suffix: String::new(),
                    });
                }
                _ => {}
            }
        }
        ts_pages.push(page);
    }

    // ---- Travel groups (speed impact groups; single page). ----
    let mut tg_views = Vec::with_capacity(travel_groups.len());
    for dto in &travel_groups {
        let (groups, raws) =
            req_groups_view(&mut *conn, "SPEED_IMPACT_GROUP", dto.id as i16, &names, tr).await?;
        let self_link = names.link(TRAVEL_GROUPS, dto.id as i64);
        for raw in &raws {
            inverse.register(raw, tr.kind_travel_group, self_link.clone(), &dto.name, tr);
        }
        let mut factors = Vec::new();
        // The mission_* columns are custom durations in seconds (applied by
        // `MissionTimeManagerBo::handle_custom_duration` when the group is
        // fixed); 0 means the mission keeps its configured base time.
        let mut factor = |k: &str, v: f64| {
            let text = if v == 0.0 {
                "0".to_string()
            } else {
                format!("{} ({})", fmt_num(v), fmt_secs(v as i64))
            };
            factors.push(Kv { k: k.into(), v: text })
        };
        factor(tr.mi_explore, dto.mission_explore);
        factor(tr.mi_gather, dto.mission_gather);
        factor(tr.mi_establish_base, dto.mission_establish_base);
        factor(tr.mi_attack, dto.mission_attack);
        factor(tr.mi_conquest, dto.mission_conquest);
        factor(tr.mi_counterattack, dto.mission_counterattack);
        let mut badges = Vec::new();
        if dto.is_fixed {
            badges.push(tr.badge_fixed_duration.into());
        }
        tg_views.push(TravelGroupView {
            id: dto.id,
            name: dto.name.clone(),
            badges,
            factors,
            mission_limits: mission_limit_rows(
                &[
                    (tr.mi_explore, &dto.can_explore),
                    (tr.mi_gather, &dto.can_gather),
                    (tr.mi_establish_base, &dto.can_establish_base),
                    (tr.mi_attack, &dto.can_attack),
                    (tr.mi_counterattack, &dto.can_counterattack),
                    (tr.mi_conquest, &dto.can_conquest),
                    (tr.mi_deploy, &dto.can_deploy),
                ],
                tr,
            ),
            req_groups: groups,
        });
    }

    // ---- Special locations. ----
    let mut sl_pages = Vec::with_capacity(special_locations.len());
    let mut sl_galaxies: HashMap<u16, Option<u16>> = HashMap::new();
    for dto in &special_locations {
        let mut page = EntityPage::new(dto.id, SPECIAL_LOCATIONS, dto.name.clone());
        page.image_url = dto.image_url.clone();
        page.description = dto.description.clone();
        sl_galaxies.insert(dto.id, dto.galaxy_id);
        // The galaxy is always shown (as the header link, and as the list-page
        // group heading); only the exact planet is gated by
        // WIKI_EXPOSE_SPECIAL_LOCATIONS. The list-row subtitle carries that
        // planet when exposed — the galaxy would just repeat the group heading.
        page.header_link = dto.galaxy_id.and_then(|g| names.link(GALAXIES, g as i64));
        if expose_special_locations
            && let Some(planet) = &dto.assigned_planet_name
        {
            page.subtitle = planet.clone();
            page.stats.push(Kv {
                k: tr.st_found_at.into(),
                v: planet.clone(),
            });
        }
        page.improvement =
            improvement_lines(&mut *conn, "special_locations", dto.id, &names, tr).await?;
        page.improvement_note = tr.note_while_owning.into();
        sl_pages.push(page);
    }

    // ---- Faction extras (no bo exposes these; plain reads). ----
    let faction_type_limits: Vec<(u16, u16, Option<u32>)> = sqlx::query_as(
        "SELECT faction_id, unit_type_id, max_count FROM factions_unit_types ORDER BY id",
    )
    .fetch_all(&mut *conn)
    .await?;
    #[derive(sqlx::FromRow)]
    struct SpawnRow {
        faction_id: u16,
        galaxy_id: u16,
        sector_range_start: Option<u32>,
        sector_range_end: Option<u32>,
        quadrant_range_start: Option<u32>,
        quadrant_range_end: Option<u32>,
    }
    let spawn_locations: Vec<SpawnRow> = sqlx::query_as(
        "SELECT faction_id, galaxy_id, sector_range_start, sector_range_end, \
                quadrant_range_start, quadrant_range_end \
         FROM faction_spawn_location ORDER BY id",
    )
    .fetch_all(&mut *conn)
    .await?;

    let mut faction_pages = Vec::with_capacity(factions.len());
    for dto in &factions {
        let starts_with = |initial: i64, production: f32| {
            tr.fac_starts_with
                .replace("{x}", &fmt_int(initial))
                .replace("{y}", &fmt_f32(production))
        };
        let mut stats = vec![
            Kv {
                k: tr.fac_primary.replace("{name}", &dto.primary_resource_name),
                v: starts_with(
                    dto.initial_primary_resource as i64,
                    dto.primary_resource_production,
                ),
            },
            Kv {
                k: tr.fac_secondary.replace("{name}", &dto.secondary_resource_name),
                v: starts_with(
                    dto.initial_secondary_resource as i64,
                    dto.secondary_resource_production,
                ),
            },
            Kv {
                k: tr.fac_energy.replace("{name}", &dto.energy_name),
                v: fmt_int(dto.initial_energy as i64),
            },
            Kv {
                k: tr.fac_max_planets.into(),
                v: fmt_int(dto.max_planets as i64),
            },
        ];
        if let Some(v) = dto.custom_primary_gather_percentage
            && v != 0.0 {
                stats.push(Kv {
                    k: tr.fac_primary_gather.into(),
                    v: format!("{}%", fmt_f32(v)),
                });
            }
        if let Some(v) = dto.custom_secondary_gather_percentage
            && v != 0.0 {
                stats.push(Kv {
                    k: tr.fac_secondary_gather.into(),
                    v: format!("{}%", fmt_f32(v)),
                });
            }
        let unit_type_limits = faction_type_limits
            .iter()
            .filter(|(fid, _, _)| *fid == dto.id)
            .map(|(_, tid, max)| Kv {
                k: names.label(UNIT_TYPES, *tid as i64),
                v: max
                    .map(|m| fmt_int(m as i64))
                    .unwrap_or_else(|| tr.unlimited.into()),
            })
            .collect();
        let spawns = spawn_locations
            .iter()
            .filter(|s| s.faction_id == dto.id)
            .map(|s| {
                let mut txt =
                    format!("{}{}", tr.galaxy_prefix, names.label(GALAXIES, s.galaxy_id as i64));
                if let (Some(a), Some(b)) = (s.sector_range_start, s.sector_range_end) {
                    txt.push_str(
                        &tr.spawn_sectors
                            .replace("{a}", &a.to_string())
                            .replace("{b}", &b.to_string()),
                    );
                }
                if let (Some(a), Some(b)) = (s.quadrant_range_start, s.quadrant_range_end) {
                    txt.push_str(
                        &tr.spawn_quadrants
                            .replace("{a}", &a.to_string())
                            .replace("{b}", &b.to_string()),
                    );
                }
                txt
            })
            .collect();
        faction_pages.push(FactionPage {
            id: dto.id,
            name: dto.name.clone(),
            href: format!("factions/{}.html", dto.id),
            image_url: dto.image_url.clone(),
            subtitle: format!(
                "{} / {} / {}",
                dto.primary_resource_name, dto.secondary_resource_name, dto.energy_name
            ),
            description: dto.description.clone().unwrap_or_default(),
            badges: if dto.hidden {
                vec![tr.badge_hidden_faction.into()]
            } else {
                Vec::new()
            },
            stats,
            improvement: improvement_lines(&mut *conn, "factions", dto.id, &names, tr).await?,
            unit_type_limits,
            spawn_locations: spawns,
            exclusive: Vec::new(),
        });
    }

    // ---- Galaxies (single page). ----
    let mut galaxy_views = Vec::with_capacity(galaxies.len());
    for dto in &galaxies {
        let total = dto.sectors as i64 * dto.quadrants as i64 * dto.num_planets as i64;
        galaxy_views.push(GalaxyView {
            id: dto.id,
            name: dto.name.clone(),
            stats: vec![
                Kv {
                    k: tr.ga_sectors.into(),
                    v: fmt_int(dto.sectors as i64),
                },
                Kv {
                    k: tr.ga_quadrants.into(),
                    v: fmt_int(dto.quadrants as i64),
                },
                Kv {
                    k: tr.ga_planets_per_quadrant.into(),
                    v: fmt_int(dto.num_planets as i64),
                },
                Kv {
                    k: tr.ga_total_planets.into(),
                    v: fmt_int(total),
                },
            ],
            // A location's galaxy is safe to reveal (only its exact planet is
            // gated), so list every galaxy's special locations unconditionally.
            special_locations: special_locations
                .iter()
                .filter(|s| s.galaxy_id == Some(dto.id))
                .filter_map(|s| names.link(SPECIAL_LOCATIONS, s.id as i64))
                .collect(),
            spawn_factions: spawn_locations
                .iter()
                .filter(|s| s.galaxy_id == dto.id)
                .filter_map(|s| names.link(FACTIONS, s.faction_id as i64))
                .collect(),
            home_only: Vec::new(),
        });
    }

    // ---- Unit types (single page). ----
    let mut ut_views = Vec::with_capacity(unit_types.len());
    for dto in &unit_types {
        let mut stats = Vec::new();
        if let Some(max) = dto.max_count {
            stats.push(Kv {
                k: tr.ut_max_per_player.into(),
                v: fmt_int(max),
            });
        }
        if let Some(shared) = &dto.share_max_count {
            stats.push(Kv {
                k: tr.ut_counts_against.into(),
                v: shared.name.clone(),
            });
        }
        if let Some(sig) = &dto.speed_impact_group {
            stats.push(Kv {
                k: tr.ut_travel_group.into(),
                v: sig.name.clone(),
            });
        }
        if let Some(rule) = &dto.attack_rule {
            stats.push(Kv {
                k: tr.ut_attack_rule.into(),
                v: rule.name.clone(),
            });
        }
        if let Some(crit) = &dto.critical_attack {
            stats.push(Kv {
                k: tr.ut_critical_attack.into(),
                v: crit.name.clone(),
            });
        }
        if dto.has_to_inherit_improvements {
            stats.push(Kv {
                k: tr.ut_improvements_key.into(),
                v: tr.ut_inherits.into(),
            });
        }
        ut_views.push(UnitTypeView {
            id: dto.id,
            name: dto.name.clone(),
            parent: dto
                .parent
                .as_ref()
                .and_then(|p| names.link(UNIT_TYPES, p.id as i64)),
            stats,
            mission_limits: mission_limit_rows(
                &[
                    (tr.mi_explore, &dto.can_explore),
                    (tr.mi_gather, &dto.can_gather),
                    (tr.mi_establish_base, &dto.can_establish_base),
                    (tr.mi_attack, &dto.can_attack),
                    (tr.mi_counterattack, &dto.can_counterattack),
                    (tr.mi_conquest, &dto.can_conquest),
                    (tr.mi_deploy, &dto.can_deploy),
                ],
                tr,
            ),
            units: units
                .iter()
                .filter(|u| u.type_id == Some(dto.id))
                .filter_map(|u| names.link(UNITS, u.id as i64))
                .collect(),
        });
    }

    // ---- Attach the inverse index. ----
    let faction_names: Vec<String> = factions.iter().map(|f| f.name.clone()).collect();
    for page in &mut unit_pages {
        page.unlocks = inverse
            .unlocks
            .remove(&(UNITS, page.id as i64))
            .unwrap_or_default();
        page.unlocks_disclaimer = has_cloned_unlocks(&page.unlocks, &faction_names);
        page.temporal_sources = temporal_sources
            .remove(&(page.id as i64))
            .unwrap_or_default();
    }
    for page in &mut upgrade_pages {
        page.unlocks = inverse
            .unlocks
            .remove(&(UPGRADES, page.id as i64))
            .unwrap_or_default();
        page.unlocks_disclaimer = has_cloned_unlocks(&page.unlocks, &faction_names);
        page.blocks = inverse.blocks.remove(&(page.id as i64)).unwrap_or_default();
    }
    for page in &mut ts_pages {
        page.unlocks = inverse
            .unlocks
            .remove(&(TIME_SPECIALS, page.id as i64))
            .unwrap_or_default();
        page.unlocks_disclaimer = has_cloned_unlocks(&page.unlocks, &faction_names);
    }
    // Special locations are conquered (own their planet), never "unlocked", so
    // they have no ObjectEnum relation — only the inverse index applies.
    for page in &mut sl_pages {
        page.unlocks = inverse
            .unlocks
            .remove(&(SPECIAL_LOCATIONS, page.id as i64))
            .unwrap_or_default();
        page.unlocks_disclaimer = has_cloned_unlocks(&page.unlocks, &faction_names);
    }
    for page in &mut faction_pages {
        page.exclusive = inverse
            .unlocks
            .remove(&(FACTIONS, page.id as i64))
            .unwrap_or_default();
    }
    for view in &mut galaxy_views {
        view.home_only = inverse
            .unlocks
            .remove(&(GALAXIES, view.id as i64))
            .unwrap_or_default();
    }

    // ---- Display ordering. ----
    // Units, upgrades and time specials keep the engine's own catalog order
    // (what the game shows: units by `order_number IS NULL, order_number, id`,
    // upgrades/time specials by id); the page vectors were built in that
    // iteration order already. Special locations have no in-game listing, so
    // alphabetical reads best.
    sl_pages.sort_by(|a, b| a.name.cmp(&b.name));

    let unit_groups = faction_groups(
        &unit_pages,
        &unit_factions,
        Some(&unit_defaults),
        &factions,
        &names,
        tr,
    );
    let upgrade_groups =
        faction_groups(&upgrade_pages, &upgrade_factions, None, &factions, &names, tr);
    let time_special_groups = faction_groups(
        &ts_pages,
        &ts_factions,
        Some(&ts_defaults),
        &factions,
        &names,
        tr,
    );
    let special_location_groups = galaxy_groups(&sl_pages, &sl_galaxies, &galaxies, &names, tr);

    let config_sections = config_sections(&mut *conn, tr).await?;

    let counts = vec![
        Kv { k: tr.units.into(), v: fmt_int(unit_pages.len() as i64) },
        Kv { k: tr.upgrades.into(), v: fmt_int(upgrade_pages.len() as i64) },
        Kv { k: tr.time_specials.into(), v: fmt_int(ts_pages.len() as i64) },
        Kv { k: tr.special_locations.into(), v: fmt_int(sl_pages.len() as i64) },
        Kv { k: tr.factions.into(), v: fmt_int(faction_pages.len() as i64) },
        Kv { k: tr.galaxies.into(), v: fmt_int(galaxy_views.len() as i64) },
        Kv { k: tr.unit_types.into(), v: fmt_int(ut_views.len() as i64) },
        Kv { k: tr.travel_groups.into(), v: fmt_int(tg_views.len() as i64) },
    ];

    Ok(Site {
        universe: universe.to_string(),
        generated_at: chrono::Local::now().format("%Y-%m-%d %H:%M:%S").to_string(),
        counts,
        units: unit_pages,
        unit_groups,
        upgrades: upgrade_pages,
        upgrade_groups,
        time_specials: ts_pages,
        time_special_groups,
        special_locations: sl_pages,
        special_location_groups,
        factions: faction_pages,
        galaxies: galaxy_views,
        unit_types: ut_views,
        travel_groups: tg_views,
        config_sections,
    })
}
