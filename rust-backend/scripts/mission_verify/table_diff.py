#!/usr/bin/env python3
"""Diff two mission-state dumps (see dump_mission_state.sh) table by table.

Both dumps are expected to describe the SAME scenario run on two backends (Java
vs Rust) against the same starting data, so any *surviving* difference after
normalisation is a porting gap.

Normalisation (the crux — surrogate ids and legit timestamps differ between the
two runs even when combat is identical, so they must be stripped, while the
RNG-derived combat numbers are KEPT because Part 1/2 make them deterministic):

  * obtained_units : drop `id`, `mission_id`, `first_deployment_mission`,
                     `expiration_id`, `owner_unit_id` (all surrogate / mission
                     fk that differ per run); sort by a stable business key.
  * missions       : drop `id`, `report_id`, `related_mission`; sort.
  * mission_reports: drop `id`; parse `json_body` and recursively drop the
                     volatile keys (`id`, `date`, `*Date`, `missionId`,
                     `reportId`) while KEEPING combat numbers (counts, captured,
                     killed, survivors, pointsGiven, ...); sort.
  * mission_information: drop `id`; sort by (mission position, relation_id).
  * scheduled_tasks: keep only `task_name` (the per-row instance/uuid differs);
                     compare as a multiset of names (return-mission count parity).
  * everything else: drop `id`; sort.

Reuses rest_sync_diff.py's `norm` / `diff` / `short` structural helpers (imported
when this script sits next to ws_verify on PYTHONPATH; otherwise an inlined copy).

Usage:
    table_diff.py JAVA.json RUST.json [--json OUT.json]
                  [--a-label Java] [--b-label Rust]
"""
import argparse
import json
import os
import sys

# --- reuse rest_sync_diff.py's structural helpers if importable ----------------
_WS = os.path.join(os.path.dirname(__file__), "..", "ws_verify")
sys.path.insert(0, os.path.abspath(_WS))
try:
    from rest_sync_diff import norm as _ws_norm, short  # noqa: F401
except Exception:  # noqa: BLE001 — keep working if ws_verify moves
    def short(v, n=80):
        s = json.dumps(v, ensure_ascii=False)
        return s if len(s) <= n else s[:n] + "…"

    def _ws_norm(v):
        if isinstance(v, dict):
            return {k: _ws_norm(x) for k, x in v.items()}
        if isinstance(v, list):
            return [_ws_norm(x) for x in v]
        return v


# Surrogate / cross-run-volatile columns to drop per table before comparing.
DROP_COLS = {
    "obtained_units": {"id", "mission_id", "first_deployment_mission",
                       "expiration_id", "owner_unit_id"},
    "missions": {"id", "report_id", "related_mission"},
    "mission_reports": {"id"},
    "mission_information": {"id"},
    # primary/secondary/energy regenerate continuously from wall-clock, so the
    # sequential Java-then-Rust runs always drift on them — they are NOT combat
    # outputs (attack missions consume resources at *creation*, not resolve).
    # Keep only `points`, which is the deterministic combat-derived field.
    "user_storage": {"primary_resource", "secondary_resource", "energy"},
    "planets": set(),
    "unlocked_relation": {"id"},
    "active_time_specials": {"id", "activation_date", "expiring_date",
                             "ready_date"},
    "scheduled_tasks": {"task_instance"},
}

# Stable sort key (business identity) per table.
SORT_KEYS = {
    "obtained_units": lambda r: (r.get("user_id"), r.get("unit_id"),
                                 r.get("source_planet"), r.get("count"),
                                 r.get("is_from_capture")),
    "missions": lambda r: (r.get("user_id"), r.get("type"),
                           r.get("source_planet"), r.get("target_planet")),
    "mission_reports": lambda r: (r.get("user_id"), r.get("is_enemy"),
                                  json.dumps(r.get("json_body"), sort_keys=True)),
    "mission_information": lambda r: (r.get("mission_id"), r.get("relation_id"),
                                      r.get("value")),
    "user_storage": lambda r: (r.get("id"),),
    "planets": lambda r: (r.get("id"),),
    "unlocked_relation": lambda r: (r.get("user_id"), r.get("relation_id")),
    "active_time_specials": lambda r: (r.get("user_id"), r.get("time_special_id"),
                                       r.get("state")),
    "scheduled_tasks": lambda r: (r.get("task_name"),),
}

# Keys inside a mission_reports json_body that legitimately differ run-to-run,
# plus non-combat cosmetic fields Java's full POJO DTOs serialise but Rust's lean
# report DTOs omit (e.g. `canAlterTwitchState` — a static user flag Jackson emits
# as the primitive-boolean default `false`; never a combat output).
REPORT_VOLATILE = {"id", "date", "missionid", "reportid", "creationdate",
                   "missiondate", "canaltertwitchstate"}


def _strip_report_json(node):
    """Recursively drop volatile keys from a parsed mission-report body, keeping
    every combat number (counts/captured/killed/survivors/points/...).

    The embedded `unit` node is the full STATIC unit-catalog DTO (name, image,
    speedImpactGroup, requirementsGroups, …). It is not a combat OUTPUT — it is
    catalog metadata echoed into the report, and Java/Rust serialise it at
    different depths (Java inlines the whole requirement tree). For *combat*
    parity we only care WHICH unit it is, so collapse `unit` to its `id`.
    """
    if isinstance(node, dict):
        out = {}
        for k, v in node.items():
            kl = k.lower()
            if kl in REPORT_VOLATILE or kl.endswith("date"):
                continue
            if kl == "unit" and isinstance(v, dict) and "id" in v:
                out[k] = {"id": v["id"]}
                continue
            out[k] = _strip_report_json(v)
        return out
    if isinstance(node, list):
        return [_strip_report_json(x) for x in node]
    return node


def normalize_table(name, rows):
    drop = DROP_COLS.get(name, {"id"})
    out = []
    for row in rows:
        r = {k: v for k, v in row.items() if k not in drop}
        if name == "mission_reports" and r.get("json_body") is not None:
            try:
                parsed = json.loads(r["json_body"])
                r["json_body"] = _strip_report_json(parsed)
            except (json.JSONDecodeError, TypeError):
                pass  # leave as raw string; it'll diff if genuinely different
        out.append(r)
    keyfn = SORT_KEYS.get(name, lambda x: json.dumps(x, sort_keys=True))

    def safe_key(r):
        try:
            return json.dumps(keyfn(r), sort_keys=True, default=str)
        except TypeError:
            return json.dumps(r, sort_keys=True, default=str)

    out.sort(key=safe_key)
    return _ws_norm(out)


def diff_rows(a_rows, b_rows):
    """Return up to a few human-readable difference lines between two already
    normalised row lists."""
    lines = []
    if len(a_rows) != len(b_rows):
        lines.append(f"  row count: A={len(a_rows)} B={len(b_rows)}")
    for i in range(min(len(a_rows), len(b_rows))):
        if a_rows[i] != b_rows[i]:
            # find diverging fields
            ak, bk = a_rows[i], b_rows[i]
            if isinstance(ak, dict) and isinstance(bk, dict):
                for f in sorted(set(ak) | set(bk)):
                    if ak.get(f) != bk.get(f):
                        lines.append(
                            f"  row[{i}].{f}: A={short(ak.get(f))} "
                            f"B={short(bk.get(f))}")
            else:
                lines.append(f"  row[{i}]: A={short(ak)} B={short(bk)}")
            if len(lines) >= 12:
                lines.append("  …(more rows differ)")
                break
    # surplus rows on either side
    if len(a_rows) > len(b_rows):
        for i in range(len(b_rows), min(len(a_rows), len(b_rows) + 3)):
            lines.append(f"  A-only row[{i}]: {short(a_rows[i])}")
    elif len(b_rows) > len(a_rows):
        for i in range(len(a_rows), min(len(b_rows), len(a_rows) + 3)):
            lines.append(f"  B-only row[{i}]: {short(b_rows[i])}")
    return lines


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("a", help="first dump (e.g. Java state json)")
    ap.add_argument("b", help="second dump (e.g. Rust state json)")
    ap.add_argument("--a-label", default="A(Java)")
    ap.add_argument("--b-label", default="B(Rust)")
    ap.add_argument("--json", default=None)
    args = ap.parse_args()

    with open(args.a) as fh:
        A = json.load(fh)
    with open(args.b) as fh:
        B = json.load(fh)

    tables = sorted(set(A) | set(B))
    results = {}
    n_match = n_diff = 0

    for t in tables:
        a_rows = normalize_table(t, A.get(t, []))
        b_rows = normalize_table(t, B.get(t, []))
        lines = diff_rows(a_rows, b_rows)
        if not lines:
            print(f"=== {t:<20} === ✅ match ({len(a_rows)} rows)")
            results[t] = {"match": True, "rows": len(a_rows)}
            n_match += 1
        else:
            print(f"\n=== {t:<20} === 🔴 DIFFERS")
            for l in lines:
                print(l)
            print()
            results[t] = {"match": False, "diffs": lines}
            n_diff += 1

    print("=" * 60)
    print(f"SUMMARY: {n_match} tables match, {n_diff} differ "
          f"({args.a_label} vs {args.b_label})")

    if args.json:
        with open(args.json, "w") as fh:
            json.dump(results, fh, indent=2, ensure_ascii=False)
        print(f"Wrote {args.json}")

    sys.exit(0 if n_diff == 0 else 1)


if __name__ == "__main__":
    main()
