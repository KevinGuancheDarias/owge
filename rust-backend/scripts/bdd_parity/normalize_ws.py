#!/usr/bin/env python3
"""Canonicalize ws deliver frames for the layer-2 diff (plan §5.4 — these are
NORMALIZATIONS, each justified, not suppressions):

1. sort JSON keys — serialization key order is not contractual (Jackson
   insertion order vs serde struct order would otherwise diff every frame);
2. wall-clock-derived VALUES (terminationDate/startingDate/creationDate/
   browsingDate/reportDate/activationDate/expiringDate/readyDate/userReadDate/
   pendingMillis) become placeholders that PRESERVE the serialization format
   AND the sub-second precision class: a Jackson [y,m,d,…] array becomes
   "<TS-ARR>", an ISO string "<TS-STR-MS>"/"<TS-STR-S>" (with/without a
   fractional part), an epoch number "<TS-NUM-MS>"/"<TS-NUM-S>" (millis
   present / whole-second, i.e. v % 1000 == 0). A format OR precision
   divergence stays visible (D16: millis are contractual per Kevin's ruling),
   only the pass-to-pass clock noise is removed. Caveat: a genuine
   millis-precision clock has a 1/1000 chance of landing on …000 and
   normalizing to <TS-NUM-S> — a lone single-key precision red on re-run is
   that flake, not a regression.

3. intra-payload ORDER of id-keyed object arrays (D17, Kevin's ruling
   2026-07-10): both sides emit the same elements in an unspecified order
   (Java = accidental unlocked_relation scan / Hibernate fill order, Rust =
   ORDER BY id), and one transposition cascades into dozens of phantom
   positional field diffs. Arrays whose EVERY element is an object carrying an
   "id" are sorted by that id on BOTH sides before the diff — content, shape,
   length and extra/missing elements still diff normally; only the position
   information is discarded. EXCEPT where order IS the contract (Kevin's
   ruling): report lists and the user's planet list — frames of the events in
   ORDERED_VALUE_EVENTS and any subtree under a key in ORDERED_KEYS keep
   positional comparison.

4. `user_data_change.value.{primaryResource,secondaryResource}` (D19): the
   balances regenerate per-second from wall-clock (and the resources Given
   resets last_action), so the two backends' runs can never agree — the same
   tolerated class as ws_verify; mission COSTS are asserted on the missions
   table rows instead. Scoped to the user_data_change payload only: the same
   key names on upgrades/units are prices and still diff.
5. the lazy-association PRESENCE class (R2, Kevin's ruling 2026-07-10): the
   depth of these nested graphs is per-session Hibernate hydration state and
   Java itself is not reproducible run-to-run (same scenario mixed deep and
   shallow frames; planet_owned_change specialLocation flipped rich/slim
   between two identical fresh-JVM runs) — byte-porting is impossible by
   construction, so BOTH sides are normalized:
   (a) `requirementsGroups` is dropped from any object sitting under a
       `speedImpactGroup` key (values inside, when both sides carry them, no
       longer compare — the top-level `speed_impact_group_unlocked_change`
       payload is NOT touched and still asserts them);
   (b) `planet_owned_change` specialLocation drops its lazy fields
       (galaxyId/galaxyName/image/imageUrl/improvement) — the slim identity
       core {id,name,description,assignedPlanet*} still compares.

Envelope fields (status, lastSent presence) are deliberately NOT normalized —
Rust adding them where Java doesn't is a real, reportable divergence.
"""
import json
import sys

DATEISH = {
    "terminationDate",
    "startingDate",
    "creationDate",
    "browsingDate",
    "reportDate",
    "activationDate",
    "expiringDate",
    "readyDate",
    "userReadDate",
}
NUMISH = {"pendingMillis"}
# D17 order-is-contractual exemptions: whole frames by eventName…
ORDERED_VALUE_EVENTS = {
    "planet_user_list_change",
    "mission_report_change",
    "mission_report_new",
}
# …and subtrees by parent key (paginated report responses embed "reports").
ORDERED_KEYS = {"reports"}


# R2: lazy fields dropped from planet_owned_change specialLocation objects.
SL_LAZY_FIELDS = ("galaxyId", "galaxyName", "image", "imageUrl", "improvement")


def norm(obj, sortable=True):
    if isinstance(obj, dict):
        out = {}
        for k, v in obj.items():
            if k in DATEISH and isinstance(v, list):
                out[k] = "<TS-ARR>"
            elif k in DATEISH and isinstance(v, str):
                out[k] = "<TS-STR-MS>" if "." in v else "<TS-STR-S>"
            elif k in DATEISH and isinstance(v, (int, float)):
                out[k] = "<TS-NUM-S>" if v % 1000 == 0 else "<TS-NUM-MS>"
            elif k in NUMISH and isinstance(v, (int, float)):
                out[k] = "<NUM>"
            elif k == "speedImpactGroup" and isinstance(v, dict):
                # R2(a): requirementsGroups presence here is session noise.
                inner = {ik: iv for ik, iv in v.items() if ik != "requirementsGroups"}
                out[k] = norm(inner, sortable)
            else:
                out[k] = norm(v, sortable and k not in ORDERED_KEYS)
        return out
    if isinstance(obj, list):
        items = [norm(v, sortable) for v in obj]
        if sortable and items and all(isinstance(x, dict) and "id" in x for x in items):
            # repr() gives a deterministic total order even for mixed/None ids;
            # only consistency between the two sides matters for the diff.
            items.sort(key=lambda x: repr(x["id"]))
        return items
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
    payload = frame.get("payload") if isinstance(frame, dict) else None
    event = payload.get("eventName") if isinstance(payload, dict) else None
    if event == "user_data_change" and isinstance(payload.get("value"), dict):
        for k in ("primaryResource", "secondaryResource"):
            if isinstance(payload["value"].get(k), (int, float)):
                payload["value"][k] = "<NUM>"
    if event == "planet_owned_change" and isinstance(payload.get("value"), list):
        # R2(b): specialLocation's lazy fields are session noise on this event.
        for planet in payload["value"]:
            sl = planet.get("specialLocation") if isinstance(planet, dict) else None
            if isinstance(sl, dict):
                for k in SL_LAZY_FIELDS:
                    sl.pop(k, None)
    print(
        json.dumps(
            norm(frame, sortable=event not in ORDERED_VALUE_EVENTS),
            sort_keys=True,
            separators=(",", ":"),
            ensure_ascii=False,
        )
    )
