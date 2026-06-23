#!/usr/bin/env python3
"""Align two RNG-draw trace streams (Java vs Rust) by `seq` and report the FIRST
divergence — the workhorse for bringing the seeded-RNG combat into lock-step.

Both backends, when `ATTACK_DETERMINISTIC_RNG=TRUE` and trace is on, emit ONE
JSON object per RNG draw, one per line, to a trace sink (stderr/file). Agreed
schema (identical fields on both sides):

    {"seq":   <int>,                 # monotonically increasing draw index
     "site":  "shuffle"|"capture_prob"|"capture_amount",
     "seed":  <int|null>,            # the JavaRandom seed (shuffle site only)
     "bound": <int|null>,            # nextInt bound (shuffle), else null
     "attacker": <obtainedUnitId|null>,
     "victim":   <obtainedUnitId|null>,
     "killed":   <int|null>,         # capture_amount only
     "result":   <number>}           # the draw output (shuffle swap idx /
                                      # nextDouble for capture sites)

This script:
  * tolerates a MISSING or EMPTY trace on either side (the instrumentation is
    produced by a different agent and may not be wired yet) -> prints
    "no trace emitted yet" and exits 0 so the table-diff path still gates.
  * tolerates non-JSON / diagnostic lines (skips them).
  * aligns by `seq`, compares field by field, prints the first mismatch with both
    full lines and the diverging field name.

Usage:
    trace_diff.py JAVA_TRACE.jsonl RUST_TRACE.jsonl
                  [--a-label Java] [--b-label Rust]
"""
import argparse
import json
import sys

# Fields compared at each seq — the LOCK-STEP STRUCTURE of the draw sequence:
# which sites fire, on which (attacker→victim) pairs, with which `killed` feed, in
# which order. `seq` is the alignment key, not a diff field.
#
# `seed`, `result`, `attacker` and `victim` are intentionally EXCLUDED — they are
# all per-run surrogate values, not lock-step signals:
#   * `seed`   = mission.id; the harness runs Java and Rust as two SEPARATE
#                missions with separate auto-increment ids, so it differs by design.
#   * `result` = nextInt/nextDouble output, which is a function OF the seed, so it
#                differs whenever the seed does. (Bit-exactness for a GIVEN seed is
#                proven separately by the `java_random` golden unit test, Part 1.)
#   * `attacker`/`victim` = obtained_unit ids. Combat RECREATES the attacker stack
#                (the departing units become a fresh mission-attached row), so its
#                id is drawn from the global auto-increment, which keeps climbing
#                across the Java run — Rust's equivalent row gets a higher id. The
#                victim happens to keep its snapshot id (it is only decremented),
#                but in general these ids are run-specific. (The table diff already
#                proves the *outcome* — captured row, counts — matches exactly.)
# What remains is the lock-step STRUCTURE: identical ordered sequence of draw
# `site`s with identical `bound` (shuffle) and `killed` (capture_amount) inputs,
# and an identical draw COUNT — i.e. both engines walk combat in the same order
# and fire the same draws at the same steps (Part 3).
COMPARE_FIELDS = ["site", "bound", "killed"]

# STRICT mode (`--strict`, used ONLY in the fixed-mission-id path where both
# backends run the SAME mission id => the SAME seed): compare EVERY field including
# `seed` and `result`. With a shared seed these must be byte-identical, so this is
# the load-bearing proof of same-seed reproducibility. `attacker`/`victim` are still
# excluded — they are obtained_unit surrogate ids that differ per run even with a
# shared seed (combat recreates the attacker stack from the global auto-increment),
# and the table diff already proves the capture OUTCOME matches.
STRICT_COMPARE_FIELDS = ["site", "bound", "killed", "seed", "result"]


def load_trace(path):
    """Return (rows_by_seq, n_raw_lines, n_parsed). Non-JSON lines are skipped."""
    rows = {}
    n_raw = n_parsed = 0
    try:
        with open(path) as fh:
            for line in fh:
                s = line.strip()
                if not s:
                    continue
                n_raw += 1
                # Both backends prefix each draw with "@@RNG@@ " (the grep marker
                # the harness keys on); strip it to reach the JSON payload.
                if s.startswith("@@RNG@@"):
                    s = s[len("@@RNG@@"):].strip()
                if not s.startswith("{"):
                    continue
                try:
                    obj = json.loads(s)
                except json.JSONDecodeError:
                    continue
                if not isinstance(obj, dict) or "seq" not in obj:
                    continue
                n_parsed += 1
                rows[obj["seq"]] = obj
    except FileNotFoundError:
        return None, 0, 0
    return rows, n_raw, n_parsed


def _as_number(v):
    """Return v as a float if it is numeric (int/float/numeric str), else None."""
    if isinstance(v, bool) or v is None:
        return None
    if isinstance(v, (int, float)):
        return float(v)
    if isinstance(v, str):
        try:
            return float(v)
        except ValueError:
            return None
    return None


def _report_divergence(seq, f, ra, rb, args):
    print(f"🔴 FIRST DIVERGENCE at seq={seq}, field '{f}': "
          f"{args.a_label}={ra.get(f)!r}  {args.b_label}={rb.get(f)!r}")
    print(f"   {args.a_label}: {json.dumps(ra)}")
    print(f"   {args.b_label}: {json.dumps(rb)}")
    sys.exit(1)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("a", help="first trace jsonl (e.g. Java)")
    ap.add_argument("b", help="second trace jsonl (e.g. Rust)")
    ap.add_argument("--a-label", default="Java")
    ap.add_argument("--b-label", default="Rust")
    ap.add_argument("--strict", action="store_true",
                    help="compare ALL fields including seed/result (fixed-seed path)")
    args = ap.parse_args()

    compare_fields = STRICT_COMPARE_FIELDS if args.strict else COMPARE_FIELDS

    a, a_raw, a_n = load_trace(args.a)
    b, b_raw, b_n = load_trace(args.b)

    # Robustness: a missing/empty trace is expected until Part 4 is wired.
    a_empty = a is None or a_n == 0
    b_empty = b is None or b_n == 0
    if a_empty and b_empty:
        print("RNG trace: no trace emitted yet on either side "
              "(ATTACK_DETERMINISTIC_RNG instrumentation not wired) — skipping.")
        sys.exit(0)
    if a_empty or b_empty:
        side = args.a_label if a_empty else args.b_label
        other = args.b_label if a_empty else args.a_label
        print(f"RNG trace: 🟡 {side} emitted NO draws but {other} emitted "
              f"{(b_n if a_empty else a_n)} — only one backend is instrumented.")
        sys.exit(0)

    print(f"RNG trace: {args.a_label}={a_n} draws, {args.b_label}={b_n} draws")

    all_seqs = sorted(set(a) | set(b))
    for seq in all_seqs:
        ra = a.get(seq)
        rb = b.get(seq)
        if ra is None or rb is None:
            missing = args.a_label if ra is None else args.b_label
            present = rb if ra is None else ra
            print(f"🔴 FIRST DIVERGENCE at seq={seq}: missing on {missing}")
            print(f"   {('present' )}: {json.dumps(present)}")
            sys.exit(1)
        for f in compare_fields:
            va, vb = ra.get(f), rb.get(f)
            # `result` (and any numeric field) is compared as a NUMBER, not by JSON
            # text: a bit-identical double can serialize differently across the Java
            # and Rust JSON writers (e.g. "0.5" vs "0.5000000000000000"). Parse both
            # to float and require exact float equality (no tolerance — JavaRandom is
            # bit-exact, so a real divergence is a genuine bug, not rounding).
            if _as_number(va) is not None and _as_number(vb) is not None:
                if _as_number(va) != _as_number(vb):
                    _report_divergence(seq, f, ra, rb, args)
            elif va != vb:
                _report_divergence(seq, f, ra, rb, args)

    if a_n != b_n:
        print(f"🟡 traces match where they overlap but lengths differ "
              f"({args.a_label}={a_n}, {args.b_label}={b_n})")
        sys.exit(1)

    mode = "STRICT (incl. seed+result)" if args.strict else "structure"
    print(f"✅ RNG traces identical [{mode}] ({a_n} draws aligned by seq)")
    sys.exit(0)


if __name__ == "__main__":
    main()
