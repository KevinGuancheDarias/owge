#!/usr/bin/env python3
"""Canonicalize ws deliver frames for the layer-2 diff (plan §5.4 — these are
NORMALIZATIONS, each justified, not suppressions):

1. sort JSON keys — serialization key order is not contractual (Jackson
   insertion order vs serde struct order would otherwise diff every frame);
2. wall-clock-derived VALUES (terminationDate/startingDate/creationDate/
   browsingDate/pendingMillis) become placeholders that PRESERVE the
   serialization format: a Jackson [y,m,d,…] array becomes "<TS-ARR>" while an
   ISO string becomes "<TS-STR>" — a format divergence stays visible, only the
   pass-to-pass clock noise is removed.

Envelope fields (status, lastSent presence) are deliberately NOT normalized —
Rust adding them where Java doesn't is a real, reportable divergence.
"""
import json
import sys

DATEISH = {"terminationDate", "startingDate", "creationDate", "browsingDate"}
NUMISH = {"pendingMillis"}


def norm(obj):
    if isinstance(obj, dict):
        out = {}
        for k, v in obj.items():
            if k in DATEISH and isinstance(v, list):
                out[k] = "<TS-ARR>"
            elif k in DATEISH and isinstance(v, str):
                out[k] = "<TS-STR>"
            elif k in DATEISH and isinstance(v, (int, float)):
                out[k] = "<TS-NUM>"
            elif k in NUMISH and isinstance(v, (int, float)):
                out[k] = "<NUM>"
            else:
                out[k] = norm(v)
        return out
    if isinstance(obj, list):
        return [norm(v) for v in obj]
    return obj


for line in sys.stdin:
    line = line.strip()
    if not line:
        continue
    try:
        frame = json.loads(line)
    except json.JSONDecodeError:
        print(line)
        continue
    print(json.dumps(norm(frame), sort_keys=True, separators=(",", ":"), ensure_ascii=False))
