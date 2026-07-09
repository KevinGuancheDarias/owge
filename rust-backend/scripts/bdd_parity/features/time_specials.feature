Feature: Time special activation
  Covers only the activation side of the time special lifecycle: successful
  activation flips the row to ACTIVE and emits the expected websocket events,
  activating a not-yet-unlocked special is rejected, and re-activating an
  already-active special is a silent idempotent no-op (no new row, no new
  websocket emission). Expiry/recharge transitions (Quartz-nudge, B6-B11) are
  out of scope here — they need a not-yet-implemented time-based nudge step.
  The B4 HAVE_SPECIAL_ENABLED grant-cascade scenario is also omitted: the only
  gating Given available is "... exists gated by requirement
  HAVE_SPECIAL_LOCATION ...", so a unit gated by HAVE_SPECIAL_ENABLED cannot
  currently be expressed. Reference: business/ActiveTimeSpecialBo.java
  (activate, B1-B5), business/RequirementBo.java. See
  inventories/time-specials.md for the full behavior catalog.

  Background:
    Given the standard test universe
    And time special 900 exists gated by requirement HAVE_SPECIAL_LOCATION with second value 500

  Scenario: Activating an unlocked time special succeeds
    # covers: B4
    Given user 1 has an unlocked relation for object TIME_SPECIAL reference 900
    When user 1 activates time special 900
    Then the request succeeded
    And table active_time_specials has a row where user_id=1 and time_special_id=900 and state=ACTIVE
    And user 1 received websocket event "time_special_change" where some item has id 900
    And user 1 received websocket event "user_improvements_change"

  Scenario: Activating a not-unlocked time special is rejected
    # covers: B2
    Given user 2 has no unlocked relation for object TIME_SPECIAL reference 900
    When user 2 attempts to activate time special 900
    Then the request is rejected with HTTP status 500
    And table active_time_specials has no row where user_id=2 and time_special_id=900

  Scenario: Activating an already-active time special is a silent no-op
    # covers: B3
    Given user 1 has an unlocked relation for object TIME_SPECIAL reference 900
    # activate twice: the second call must be a silent no-op (no duplicate row).
    # ("no event on the SECOND call" is not expressible — the negative ws step
    # is scenario-cumulative and the first activation legitimately emits.)
    When user 1 activates time special 900
    And user 1 activates time special 900
    Then the request succeeded
    And table active_time_specials has 1 rows where user_id=1 and time_special_id=900 and state=ACTIVE
