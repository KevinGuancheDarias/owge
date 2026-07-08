//! Askama templates + the site writer. The site is rendered once per language
//! into `<out>/{en,es}/…` (each subtree is fully self-contained), with a tiny
//! browser-language redirect page at `<out>/index.html` — the same
//! translate-the-chrome model as the game frontend. Rendering goes into a
//! sibling `<out>.building` directory which is then swapped in, so a webserver
//! never serves a half-written site.

use std::fs;
use std::path::{Path, PathBuf};

use anyhow::{Context, Result};
use askama::Template;

use crate::i18n::{LANGS, LangDef, Tr};
use crate::view::*;

/// Fields every page shares; the base template renders from these.
pub struct Meta {
    pub title: String,
    /// Prefix from the page's directory back to the language root: "" or "../".
    pub root: &'static str,
    /// Which nav entry to highlight.
    pub active: &'static str,
    pub universe: String,
    /// Fully-rendered footer line (translated, timestamp substituted).
    pub footer: String,
    /// BCP-47 code of this page's language (the `<html lang>` value).
    pub lang: &'static str,
    /// Link to this same page in the other language.
    pub switch_href: String,
    pub switch_label: &'static str,
    pub t: &'static Tr,
}

#[derive(Template)]
#[template(path = "index.html")]
struct IndexT<'a> {
    meta: Meta,
    site: &'a Site,
}

#[derive(Template)]
#[template(path = "list.html")]
struct ListT<'a> {
    meta: Meta,
    heading: String,
    blurb: String,
    rows: &'a [ListRow],
}

#[derive(Template)]
#[template(path = "grouped_list.html")]
struct GroupedListT<'a> {
    meta: Meta,
    heading: String,
    blurb: String,
    groups: &'a [ListGroup],
}

#[derive(Template)]
#[template(path = "entity.html")]
struct EntityT<'a> {
    meta: Meta,
    e: &'a EntityPage,
}

#[derive(Template)]
#[template(path = "faction.html")]
struct FactionT<'a> {
    meta: Meta,
    f: &'a FactionPage,
}

#[derive(Template)]
#[template(path = "galaxies.html")]
struct GalaxiesT<'a> {
    meta: Meta,
    galaxies: &'a [GalaxyView],
}

#[derive(Template)]
#[template(path = "unit_types.html")]
struct UnitTypesT<'a> {
    meta: Meta,
    types: &'a [UnitTypeView],
}

#[derive(Template)]
#[template(path = "travel_groups.html")]
struct TravelGroupsT<'a> {
    meta: Meta,
    groups: &'a [TravelGroupView],
}

#[derive(Template)]
#[template(path = "configuration.html")]
struct ConfigurationT<'a> {
    meta: Meta,
    sections: &'a [ConfigSection],
}

/// Per-language render context.
struct Ctx<'a> {
    site: &'a Site,
    lang: &'static LangDef,
    other: &'static LangDef,
}

impl Ctx<'_> {
    /// `path` is the page's language-root-relative location; `root` its
    /// prefix back to the language root ("" or "../").
    fn meta(&self, title: &str, root: &'static str, active: &'static str, path: &str) -> Meta {
        Meta {
            title: title.to_string(),
            root,
            active,
            universe: self.site.universe.clone(),
            footer: self
                .lang
                .tr
                .footer
                .replace("{ts}", &self.site.generated_at),
            lang: self.lang.code,
            switch_href: format!("{root}../{}/{path}", self.other.code),
            switch_label: self.other.label,
            t: self.lang.tr,
        }
    }
}

fn write(path: PathBuf, content: String) -> Result<()> {
    fs::write(&path, content).with_context(|| format!("writing {}", path.display()))
}

/// Render one language's whole subtree into `dir` (which must already exist).
fn render_lang(ctx: &Ctx, dir: &Path) -> Result<()> {
    let site = ctx.site;
    let tr = ctx.lang.tr;
    for sub in [
        "units",
        "upgrades",
        "time-specials",
        "special-locations",
        "factions",
    ] {
        fs::create_dir_all(dir.join(sub))?;
    }

    write(
        dir.join("index.html"),
        IndexT {
            meta: ctx.meta(tr.overview, "", "home", "index.html"),
            site,
        }
        .render()?,
    )?;

    // Grouped list pages: units, upgrades and time specials by faction;
    // special locations by galaxy.
    let grouped_sections: [(&str, &'static str, &str, &[ListGroup]); 4] = [
        (tr.units, "units", tr.blurb_units, &site.unit_groups),
        (
            tr.upgrades,
            "upgrades",
            tr.blurb_upgrades,
            &site.upgrade_groups,
        ),
        (
            tr.time_specials,
            "time-specials",
            tr.blurb_time_specials,
            &site.time_special_groups,
        ),
        (
            tr.special_locations,
            "special-locations",
            tr.blurb_special_locations,
            &site.special_location_groups,
        ),
    ];
    for (heading, section, blurb, groups) in grouped_sections {
        let list_path = format!("{section}/index.html");
        write(
            dir.join(&list_path),
            GroupedListT {
                meta: ctx.meta(heading, "../", section_key(section), &list_path),
                heading: heading.to_string(),
                blurb: blurb.to_string(),
                groups,
            }
            .render()?,
        )?;
    }

    // Detail pages for every entity section.
    let detail_sections: [(&'static str, &[EntityPage]); 4] = [
        ("units", &site.units),
        ("upgrades", &site.upgrades),
        ("time-specials", &site.time_specials),
        ("special-locations", &site.special_locations),
    ];
    for (section, pages) in detail_sections {
        for page in pages {
            write(
                dir.join(&page.href),
                EntityT {
                    meta: ctx.meta(&page.name, "../", section_key(section), &page.href),
                    e: page,
                }
                .render()?,
            )?;
        }
    }

    let faction_rows: Vec<ListRow> = site
        .factions
        .iter()
        .map(|f| ListRow {
            href: f.href.clone(),
            image_url: f.image_url.clone(),
            name: f.name.clone(),
            subtitle: f.subtitle.clone(),
            badges: f.badges.clone(),
        })
        .collect();
    write(
        dir.join("factions/index.html"),
        ListT {
            meta: ctx.meta(tr.factions, "../", "factions", "factions/index.html"),
            heading: tr.factions.to_string(),
            blurb: tr.blurb_factions.to_string(),
            rows: &faction_rows,
        }
        .render()?,
    )?;
    for f in &site.factions {
        write(
            dir.join(&f.href),
            FactionT {
                meta: ctx.meta(&f.name, "../", "factions", &f.href),
                f,
            }
            .render()?,
        )?;
    }

    write(
        dir.join("galaxies.html"),
        GalaxiesT {
            meta: ctx.meta(tr.galaxies, "", "galaxies", "galaxies.html"),
            galaxies: &site.galaxies,
        }
        .render()?,
    )?;
    write(
        dir.join("unit-types.html"),
        UnitTypesT {
            meta: ctx.meta(tr.unit_types, "", "unit-types", "unit-types.html"),
            types: &site.unit_types,
        }
        .render()?,
    )?;
    write(
        dir.join("travel-groups.html"),
        TravelGroupsT {
            meta: ctx.meta(tr.travel_groups, "", "travel-groups", "travel-groups.html"),
            groups: &site.travel_groups,
        }
        .render()?,
    )?;
    write(
        dir.join("configuration.html"),
        ConfigurationT {
            meta: ctx.meta(tr.configuration, "", "configuration", "configuration.html"),
            sections: &site.config_sections,
        }
        .render()?,
    )?;

    fs::write(dir.join("style.css"), include_str!("../assets/style.css"))?;
    fs::write(dir.join("search.js"), include_str!("../assets/search.js"))?;
    write(dir.join("search-index.js"), search_index(site, tr)?)?;
    Ok(())
}

fn section_key(section: &str) -> &'static str {
    match section {
        "units" => "units",
        "upgrades" => "upgrades",
        "time-specials" => "time-specials",
        "special-locations" => "special-locations",
        _ => "factions",
    }
}

fn search_index(site: &Site, tr: &Tr) -> Result<String> {
    let mut entries: Vec<serde_json::Value> = Vec::new();
    let mut add = |t: &str, h: &str, k: &str| {
        entries.push(serde_json::json!({"t": t, "h": h, "k": k}));
    };
    for p in &site.units {
        add(&p.name, &p.href, tr.kind_unit);
    }
    for p in &site.upgrades {
        add(&p.name, &p.href, tr.kind_upgrade);
    }
    for p in &site.time_specials {
        add(&p.name, &p.href, tr.kind_time_special);
    }
    for p in &site.special_locations {
        add(&p.name, &p.href, tr.kind_special_location);
    }
    for f in &site.factions {
        add(&f.name, &f.href, tr.kind_faction);
    }
    for g in &site.galaxies {
        add(&g.name, &format!("galaxies.html#g{}", g.id), tr.kind_galaxy);
    }
    for t in &site.unit_types {
        add(
            &t.name,
            &format!("unit-types.html#t{}", t.id),
            tr.kind_unit_type,
        );
    }
    for s in &site.travel_groups {
        add(
            &s.name,
            &format!("travel-groups.html#s{}", s.id),
            tr.kind_travel_group,
        );
    }
    Ok(format!(
        "window.OWGE_SEARCH={};",
        serde_json::to_string(&entries)?
    ))
}

/// The `<out>/index.html` language dispatcher: redirects to the browser's
/// language (Spanish → `es/`, anything else → `en/`), with plain links as the
/// no-JS fallback. Targets are explicit `index.html` paths because the game's
/// nginx serves `/dynamic/` with `index fake` (no directory indexes).
fn root_redirect(universe: &str) -> String {
    format!(
        "<!DOCTYPE html><html><head><meta charset=\"utf-8\">\
         <title>{universe} wiki</title>\
         <script>location.replace((navigator.language||'en').toLowerCase().startsWith('es')?'es/index.html':'en/index.html');</script>\
         </head><body>\
         <p><a href=\"en/index.html\">English</a> · <a href=\"es/index.html\">Español</a></p>\
         </body></html>"
    )
}

/// Full atomic publish: render every language into `<out>.building`, then
/// swap directories.
pub fn write_site(sites: &[(&'static LangDef, &Site)], out: &Path) -> Result<()> {
    let building = sibling(out, ".building");
    let old = sibling(out, ".old");
    if building.exists() {
        fs::remove_dir_all(&building)?;
    }
    fs::create_dir_all(&building)?;
    let universe = sites
        .first()
        .map(|(_, s)| s.universe.clone())
        .unwrap_or_default();
    for (lang, site) in sites.iter() {
        // Two languages: the switcher always targets the other one.
        let other = LANGS
            .iter()
            .find(|l| l.code != lang.code)
            .expect("at least two languages defined");
        let dir = building.join(lang.code);
        fs::create_dir_all(&dir)?;
        render_lang(
            &Ctx {
                site,
                lang,
                other,
            },
            &dir,
        )?;
    }
    fs::write(building.join("index.html"), root_redirect(&universe))?;
    if old.exists() {
        fs::remove_dir_all(&old)?;
    }
    if out.exists() {
        fs::rename(out, &old)?;
    }
    fs::rename(&building, out)
        .with_context(|| format!("activating the new site at {}", out.display()))?;
    if old.exists() {
        fs::remove_dir_all(&old)?;
    }
    Ok(())
}

fn sibling(out: &Path, suffix: &str) -> PathBuf {
    let name = out
        .file_name()
        .map(|n| n.to_string_lossy().to_string())
        .unwrap_or_else(|| "wiki".into());
    out.with_file_name(format!("{name}{suffix}"))
}
