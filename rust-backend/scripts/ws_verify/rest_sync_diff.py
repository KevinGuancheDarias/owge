#!/usr/bin/env python3
"""Compare the REST `/game/websocket-sync?keys=<key>` payloads of the Java and
Rust backends, one key at a time, and report where Rust returns *less* data.

Both backends are expected to run against the SAME database, so any difference
is a porting gap, not a data difference.

Usage:
    rest_sync_diff.py --java-base URL --rust-base URL --jwt TOKEN \
        [--keys k1,k2,...] [--json OUT.json]

Response shape (both backends): {"<key>": {"data": <payload>, "lastSent": <ts>}}
We compare only `.data`. `lastSent` and other volatile fields are ignored.
"""
import argparse
import json
import sys
import urllib.request
import urllib.error

# The 20 keys both backends register for the HTTP sync path.
DEFAULT_KEYS = [
    "user_data_change", "planet_owned_change", "planet_user_list_change",
    "unit_type_change", "unit_obtained_change", "unit_unlocked_change",
    "unit_requirements_change", "upgrade_types_change", "obtained_upgrades_change",
    "running_upgrade_change", "time_special_change", "system_message_change",
    "tutorial_entries_change", "visited_tutorial_entry_change",
    "speed_impact_group_unlocked_change", "unit_mission_change",
    "enemy_mission_change", "missions_count_change", "unit_build_mission_change",
    "mission_report_change",
]

# Fields ignored at any depth (volatile / clock-dependent / known-irrelevant).
IGNORED_FIELDS = {"lastSent"}


def fetch_all(base, jwt, keys):
    """GET *all* keys in a single request (avoids the server-side per-IP
    websocket-sync rate limiter, and matches how the frontend hydrates).

    Returns (status, parsed-map-or-None, raw-text). parsed-map shape:
    {"<key>": {"data": ..., "lastSent": ...}, ...}; unhandled keys are absent.
    """
    url = f"{base.rstrip('/')}/game/websocket-sync?keys={','.join(keys)}"
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {jwt}"})
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            body = resp.read().decode("utf-8")
            status = resp.status
    except urllib.error.HTTPError as e:
        return e.code, None, e.read().decode("utf-8", "replace")
    except Exception as e:  # noqa: BLE001
        return -1, None, str(e)
    try:
        return status, json.loads(body), body
    except json.JSONDecodeError:
        return status, None, body


def extract(parsed_map, key):
    """Pull one key's `.data` out of a batched response map.

    Returns ("__ABSENT__", None) if the server did not handle the key at all,
    else the (normalised-later) data payload.
    """
    envelope = parsed_map.get(key)
    if envelope is None:
        return ("__ABSENT__", None)
    return envelope.get("data")


def norm(v):
    """Normalise a JSON value for order-insensitive structural comparison.

    - dicts: drop ignored fields, recurse.
    - lists of dicts with an 'id': sort by id so DB row-order doesn't matter.
    - other lists: recurse element-wise (kept in order).
    """
    if isinstance(v, dict):
        return {k: norm(val) for k, val in v.items() if k not in IGNORED_FIELDS}
    if isinstance(v, list):
        items = [norm(x) for x in v]
        if items and all(isinstance(x, dict) and "id" in x for x in items):
            items = sorted(items, key=lambda x: json.dumps(x.get("id"), sort_keys=True))
        return items
    return v


def diff(java, rust, path=""):
    """Yield human-readable difference lines. Tags Rust-is-less specially."""
    out = []
    if isinstance(java, dict) and isinstance(rust, dict):
        for k in sorted(set(java) | set(rust)):
            p = f"{path}.{k}" if path else k
            if k not in rust:
                out.append(f"  [RUST MISSING FIELD] {p}  (java={short(java[k])})")
            elif k not in java:
                out.append(f"  [rust-only field]    {p}  (rust={short(rust[k])})")
            else:
                out += diff(java[k], rust[k], p)
        return out
    if isinstance(java, list) and isinstance(rust, list):
        if len(java) != len(rust):
            tag = "RUST FEWER ITEMS" if len(rust) < len(java) else "rust more items"
            out.append(f"  [{tag}] {path}[]  java={len(java)} rust={len(rust)}")
        for i in range(min(len(java), len(rust))):
            out += diff(java[i], rust[i], f"{path}[{i}]")
        return out
    if java != rust:
        # Rust null/empty where Java has a value == "less data"
        if rust in (None, "", [], {}) and java not in (None, "", [], {}):
            out.append(f"  [RUST EMPTY/NULL]    {path}  java={short(java)} rust={short(rust)}")
        else:
            out.append(f"  [VALUE DIFF]         {path}  java={short(java)} rust={short(rust)}")
    return out


def short(v, n=80):
    s = json.dumps(v, ensure_ascii=False)
    return s if len(s) <= n else s[:n] + "…"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--java-base", required=True, help="e.g. http://127.0.0.1:8081/game_api")
    ap.add_argument("--rust-base", required=True, help="e.g. http://127.0.0.1:8080")
    ap.add_argument("--jwt", required=True)
    ap.add_argument("--keys", default=None, help="comma-separated; default = all 20")
    ap.add_argument("--json", default=None, help="write machine-readable result here")
    args = ap.parse_args()

    keys = args.keys.split(",") if args.keys else DEFAULT_KEYS
    results = {}
    n_match = n_diff = n_error = 0

    for key in keys:
        # One key per request so a 500 on a single key (e.g. a decode bug) is
        # isolated rather than tanking every other key. The Java rate limiter
        # must be disabled (OWGE_WS_SYNC_RATELIMIT_PER_MINUTE=0) for this.
        js, jmap, jraw = fetch_all(args.java_base, args.jwt, [key])
        rs, rmap, rraw = fetch_all(args.rust_base, args.jwt, [key])
        entry = {"java_status": js, "rust_status": rs}

        if js != 200 or jmap is None or rs != 200 or rmap is None:
            entry["error"] = "non-200"
            print(f"\n=== {key} ===  ⚠️  HTTP java={js} rust={rs}")
            if js != 200:
                print(f"    java: {jraw[:200]}")
            if rs != 200:
                print(f"    rust: {rraw[:200]}")
            results[key] = entry
            n_error += 1
            continue

        jdata = extract(jmap, key)
        rdata = extract(rmap, key)

        # absent-key markers
        j_absent = isinstance(jdata, tuple) and jdata[0] == "__ABSENT__"
        r_absent = isinstance(rdata, tuple) and rdata[0] == "__ABSENT__"
        jd = None if j_absent else norm(jdata)
        rd = None if r_absent else norm(rdata)

        lines = []
        if j_absent or r_absent:
            if r_absent and not j_absent:
                lines = ["  [RUST DID NOT RETURN KEY] (Java did)"]
            elif j_absent and not r_absent:
                lines = ["  [rust returned key, java did not]"]
        else:
            lines = diff(jd, rd)

        if not lines:
            print(f"=== {key} ===  ✅ match")
            entry["match"] = True
            n_match += 1
        else:
            rust_less = sum(1 for l in lines if "RUST MISSING" in l or "RUST FEWER" in l or "RUST EMPTY" in l or "DID NOT RETURN" in l)
            flag = "🔴 RUST RETURNS LESS" if rust_less else "🟡 differs"
            print(f"\n=== {key} ===  {flag}  ({len(lines)} diff line(s))")
            for l in lines[:60]:
                print(l)
            if len(lines) > 60:
                print(f"  … +{len(lines) - 60} more")
            entry["match"] = False
            entry["diffs"] = lines
            entry["rust_less_count"] = rust_less
            n_diff += 1

        results[key] = entry

    print("\n" + "=" * 60)
    print(f"SUMMARY: {n_match} match, {n_diff} differ, {n_error} error (of {len(keys)} keys)")
    less = [k for k, e in results.items() if e.get("rust_less_count")]
    if less:
        print(f"Rust returns LESS data for: {', '.join(less)}")

    if args.json:
        with open(args.json, "w") as fh:
            json.dump(results, fh, indent=2, ensure_ascii=False)
        print(f"Wrote {args.json}")

    sys.exit(0 if n_diff == 0 and n_error == 0 else 1)


if __name__ == "__main__":
    main()
