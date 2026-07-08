//! The fully-resolved view model the templates render. Everything here is
//! pre-formatted text plus site-root-relative links (`Link.href` never starts
//! with `/` or `../`; templates prefix `meta.root`), so the templates stay
//! dumb and the resolve layer (`build.rs`) owns every game-semantic decision.

/// A link to another wiki page, relative to the site root.
#[derive(Debug, Clone)]
pub struct Link {
    pub href: String,
    pub label: String,
}

/// A key/value stat row.
#[derive(Debug, Clone)]
pub struct Kv {
    pub k: String,
    pub v: String,
}

/// One human-readable requirement: `prefix` + optional linked entity + `suffix`,
/// e.g. `"Have upgrade "` + link(Laser) + `" at level 3 or higher"`.
#[derive(Debug, Clone)]
pub struct ReqLine {
    pub prefix: String,
    pub link: Option<Link>,
    pub suffix: String,
}

/// An AND-group of requirements. Multiple groups on one page are alternative
/// (OR) unlock paths — mirroring the engine: slave `REQUIREMENT_GROUP`
/// relations are OR'd, requirements inside one relation are AND'd.
#[derive(Debug, Clone)]
pub struct ReqGroupView {
    /// Group name from `requirement_group.name`; empty for the implicit group
    /// formed by direct requirements.
    pub name: String,
    pub reqs: Vec<ReqLine>,
}

/// One inverse-index entry: "this content references the current page in its
/// requirements" (special location page → the units it unlocks, etc.).
#[derive(Debug, Clone)]
pub struct UnlockLine {
    /// Content kind label ("Unit", "Upgrade", "Time special", "Travel group").
    pub kind: String,
    pub link: Option<Link>,
    /// Fallback label when there is no page to link.
    pub label: String,
    /// Requirement-specific detail, e.g. "at level ≥ 3" or "needs 5 of them".
    pub detail: String,
}

/// One rule-driven effect of a time special: up to two linked entities in a
/// sentence, e.g. `"Your "` + link(Naves) + `" travel in group "` + link(Fast).
#[derive(Debug, Clone)]
pub struct EffectLine {
    pub prefix: String,
    pub link: Option<Link>,
    pub mid: String,
    pub link2: Option<Link>,
    pub suffix: String,
}

/// An attack-rule entry rendered for display.
#[derive(Debug, Clone)]
pub struct RuleEntryView {
    pub can: bool,
    /// "Unit: Death star" / "Unit type: Defenses".
    pub target: String,
}

#[derive(Debug, Clone)]
pub struct RuleView {
    pub name: String,
    /// Where the rule comes from: "own rule" or "inherited from unit type".
    pub source: String,
    pub entries: Vec<RuleEntryView>,
}

#[derive(Debug, Clone)]
pub struct CritEntryView {
    pub target: String,
    /// Formatted multiplier, e.g. "×2.5".
    pub value: String,
}

#[derive(Debug, Clone)]
pub struct CritView {
    pub name: String,
    pub source: String,
    pub entries: Vec<CritEntryView>,
}

/// One chip on the attackable-types panel: a used unit type and whether the
/// unit's effective attack rule lets it attack that type.
#[derive(Debug, Clone)]
pub struct AttackableChip {
    pub link: Link,
    pub can: bool,
}

/// The combat block of a unit page.
#[derive(Debug, Clone, Default)]
pub struct CombatView {
    pub attack_rule: Option<RuleView>,
    /// The game's "Attackable types" panel: every *used* unit type with the
    /// resolved verdict (first rule entry matching the type or an ancestor
    /// wins; no entry = attackable). Mirrors the frontend's
    /// `UnitTypeService.canAttack`, which only evaluates UNIT_TYPE entries.
    pub attackable_types: Vec<AttackableChip>,
    pub critical: Option<CritView>,
    /// The unit's effective travel (speed impact) group, engine-resolved.
    pub speed_group: Option<Link>,
    /// Travel groups this unit can intercept.
    pub intercepts: Vec<Link>,
    /// Units able to intercept this unit (they intercept its travel group).
    pub intercepted_by: Vec<Link>,
}

/// A detail page for a unit / upgrade / time special / special location.
#[derive(Debug, Clone)]
pub struct EntityPage {
    pub id: u16,
    pub name: String,
    /// Site-root-relative page path, e.g. "units/12.html".
    pub href: String,
    pub image_url: Option<String>,
    /// Short line under the name on list pages (type, cost, galaxy...).
    pub subtitle: String,
    pub description: String,
    pub badges: Vec<String>,
    /// Extra header link (a unit's type, a special location's galaxy).
    pub header_link: Option<Link>,
    pub stats: Vec<Kv>,
    pub req_groups: Vec<ReqGroupView>,
    pub unlocks: Vec<UnlockLine>,
    /// True when `unlocks` contains per-faction clones of the same content
    /// (the engine only supports OR requirement groups for travel groups, so
    /// admins re-create an entity once per faction) — shows a disclaimer.
    pub unlocks_disclaimer: bool,
    /// Content that requires this upgrade *below* some level — leveling it
    /// locks these (UPGRADE_LEVEL_LOWER_THAN inverse).
    pub blocks: Vec<UnlockLine>,
    pub improvement: Vec<String>,
    /// e.g. " (per level)" for upgrades.
    pub improvement_note: String,
    /// Time-special pages: rule-driven effects while the special is active —
    /// temporal unit grants, unit hiding, travel-group swaps.
    pub effects: Vec<EffectLine>,
    /// Unit pages: time specials that grant this unit temporarily (inverse of
    /// `temporal_units`).
    pub temporal_sources: Vec<ReqLine>,
    pub combat: Option<CombatView>,
}

impl EntityPage {
    pub fn new(id: u16, section: &'static str, name: String) -> Self {
        Self {
            id,
            href: format!("{section}/{id}.html"),
            name,
            image_url: None,
            subtitle: String::new(),
            description: String::new(),
            badges: Vec::new(),
            header_link: None,
            stats: Vec::new(),
            req_groups: Vec::new(),
            unlocks: Vec::new(),
            unlocks_disclaimer: false,
            blocks: Vec::new(),
            improvement: Vec::new(),
            improvement_note: String::new(),
            effects: Vec::new(),
            temporal_sources: Vec::new(),
            combat: None,
        }
    }
}

/// A faction detail page.
#[derive(Debug, Clone)]
pub struct FactionPage {
    pub id: u16,
    pub name: String,
    pub href: String,
    pub image_url: Option<String>,
    pub subtitle: String,
    pub description: String,
    pub badges: Vec<String>,
    pub stats: Vec<Kv>,
    pub improvement: Vec<String>,
    /// Per-unit-type build limits from `factions_unit_types`.
    pub unit_type_limits: Vec<Kv>,
    /// Rendered spawn locations from `faction_spawn_location`.
    pub spawn_locations: Vec<String>,
    /// Content gated on BEEN_RACE = this faction.
    pub exclusive: Vec<UnlockLine>,
}

/// One galaxy on the single galaxies page (anchor `#g<id>`).
#[derive(Debug, Clone)]
pub struct GalaxyView {
    pub id: u16,
    pub name: String,
    pub stats: Vec<Kv>,
    pub special_locations: Vec<Link>,
    pub spawn_factions: Vec<Link>,
    /// Content gated on HOME_GALAXY = this galaxy.
    pub home_only: Vec<UnlockLine>,
}

/// One unit type on the single unit-types page (anchor `#t<id>`).
#[derive(Debug, Clone)]
pub struct UnitTypeView {
    pub id: u16,
    pub name: String,
    pub parent: Option<Link>,
    pub stats: Vec<Kv>,
    /// can_* mission permissions worth showing (non-ANY only).
    pub mission_limits: Vec<Kv>,
    pub units: Vec<Link>,
}

/// One speed-impact group on the single travel-groups page (anchor `#s<id>`).
#[derive(Debug, Clone)]
pub struct TravelGroupView {
    pub id: u16,
    pub name: String,
    pub badges: Vec<String>,
    /// Mission duration multipliers.
    pub factors: Vec<Kv>,
    /// can_* mission permissions worth showing (non-ANY only).
    pub mission_limits: Vec<Kv>,
    pub req_groups: Vec<ReqGroupView>,
}

/// One universe-configuration setting rendered human-readably; the raw
/// `configuration` rows backing it (param name → stored value) sit behind a
/// collapsed "raw values" details block. Only non-privileged rows ever get
/// this far — privileged ones (secrets) are filtered at the query.
#[derive(Debug, Clone)]
pub struct ConfigEntry {
    pub label: String,
    pub value: String,
    pub raws: Vec<Kv>,
}

/// A titled group on the single configuration page (anchor `#c<slug>`).
#[derive(Debug, Clone)]
pub struct ConfigSection {
    pub slug: String,
    pub title: String,
    pub entries: Vec<ConfigEntry>,
}

/// A row on a section list page.
#[derive(Debug, Clone)]
pub struct ListRow {
    pub href: String,
    pub image_url: Option<String>,
    pub name: String,
    pub subtitle: String,
    pub badges: Vec<String>,
}

/// A sub-bucket inside a faction group: "Unlocked by default" (only
/// BEEN_RACE / UPGRADE_LEVEL requirements) vs "Requires further unlocks".
#[derive(Debug, Clone)]
pub struct ListSubgroup {
    pub title: String,
    pub rows: Vec<ListRow>,
}

/// A group on a grouped list page (units / upgrades / time specials by their
/// BEEN_RACE faction, the "Any faction" group collecting those without one;
/// special locations by their galaxy). `link` is the group heading's own page
/// (the faction or galaxy) when there is one. Either `rows` (flat group) or
/// `subgroups` is populated, never both.
#[derive(Debug, Clone)]
pub struct ListGroup {
    pub title: String,
    /// The faction's page, absent for the "Any faction" group.
    pub link: Option<Link>,
    pub rows: Vec<ListRow>,
    pub subgroups: Vec<ListSubgroup>,
}

impl ListGroup {
    /// Total entities in the group, across subgroups when present.
    pub fn count(&self) -> usize {
        if self.subgroups.is_empty() {
            self.rows.len()
        } else {
            self.subgroups.iter().map(|s| s.rows.len()).sum()
        }
    }
}

/// The whole resolved site.
#[derive(Debug, Clone)]
pub struct Site {
    pub universe: String,
    pub generated_at: String,
    pub counts: Vec<Kv>,
    pub units: Vec<EntityPage>,
    /// Units list page grouped by required faction, subgrouped by unlock kind.
    pub unit_groups: Vec<ListGroup>,
    pub upgrades: Vec<EntityPage>,
    /// Upgrades list page grouped by required faction.
    pub upgrade_groups: Vec<ListGroup>,
    pub time_specials: Vec<EntityPage>,
    /// Time-specials list page grouped by required faction.
    pub time_special_groups: Vec<ListGroup>,
    pub special_locations: Vec<EntityPage>,
    /// Special-locations list page grouped by galaxy (or a single ungrouped
    /// bucket when the galaxy assignment is secret).
    pub special_location_groups: Vec<ListGroup>,
    pub factions: Vec<FactionPage>,
    pub galaxies: Vec<GalaxyView>,
    pub unit_types: Vec<UnitTypeView>,
    pub travel_groups: Vec<TravelGroupView>,
    /// The universe configuration page (non-privileged settings only).
    pub config_sections: Vec<ConfigSection>,
}

/// `12 345 678` — thousands separated with a narrow space.
pub fn fmt_int(n: i64) -> String {
    let raw = n.abs().to_string();
    let mut out = String::with_capacity(raw.len() + raw.len() / 3 + 1);
    if n < 0 {
        out.push('-');
    }
    let first = raw.len() % 3;
    for (i, c) in raw.chars().enumerate() {
        if i != 0 && (i + 3 - first).is_multiple_of(3) {
            out.push('\u{202f}');
        }
        out.push(c);
    }
    out
}

/// `3d 4h 5m 6s`, dropping zero components (but always at least the seconds).
pub fn fmt_secs(total: i64) -> String {
    if total <= 0 {
        return "0s".into();
    }
    let (d, rem) = (total / 86_400, total % 86_400);
    let (h, rem) = (rem / 3_600, rem % 3_600);
    let (m, s) = (rem / 60, rem % 60);
    let mut parts = Vec::new();
    if d > 0 {
        parts.push(format!("{d}d"));
    }
    if h > 0 {
        parts.push(format!("{h}h"));
    }
    if m > 0 {
        parts.push(format!("{m}m"));
    }
    if s > 0 || parts.is_empty() {
        parts.push(format!("{s}s"));
    }
    parts.join(" ")
}

/// Shortest decimal form of an f64 (`2.5`, `3`), for multipliers/percentages.
pub fn fmt_num(v: f64) -> String {
    if v == v.trunc() && v.abs() < 1e15 {
        fmt_int(v as i64)
    } else {
        format!("{v}")
    }
}

/// Like [`fmt_num`] for f32 columns. Never widen an f32 to f64 before
/// formatting — `0.15f32 as f64` prints as `0.15000000596046448`.
pub fn fmt_f32(v: f32) -> String {
    if v == v.trunc() && v.abs() < 1e9 {
        fmt_int(v as i64)
    } else {
        format!("{v}")
    }
}
