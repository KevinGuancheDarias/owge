Feature: Mission travel — deploy, establish-base edge cases, and returns
  Covers the DEPLOY and ESTABLISH_BASE travel/ownership missions: DEPLOY's
  own-planet vs. foreign-planet (DEPLOYED marker) landing behavior and the
  registration-time orphan-DEPLOYED-stack regression (commit 83a0ab9a);
  ESTABLISH_BASE's already-owned-target edge case and the RETURN_MISSION it
  spawns; the generic registration-rejection guards shared by DEPLOY/
  ESTABLISH_BASE/CONQUEST. Combat/RNG-dependent paths (defended-planet
  conquest, max-planets cap — not reachable via the `configuration` Given,
  see inventories/missions-travel.md §"Open questions") are out of scope.
  Reference: inventories/missions-travel.md.

  Background:
    Given the standard test universe
    And user 1 has 5 units of id 10 on planet 1002
    And user 1 has explored planet 1234

  Scenario: Deploying to your own planet lands the stack directly, no DEPLOYED mission
    # covers: B4(not-triggered), B23(not-triggered: different planets), B38
    # baseline pre-seeds unit-10 stacks on 1003 — wipe for an exact landing count
    Given user 1 has 0 units of id 10 on planet 1003
    When user 1 runs a DEPLOY mission from planet 1002 to planet 1003 with 5 units of id 10
    Then user 1 has 5 units of id 10 on planet 1003
    And table missions has no row where user_id=1 and type_code=DEPLOYED and target_planet=1003

  Scenario: Deploying to a foreign planet creates a DEPLOYED marker mission
    # covers: B40, B41
    When user 1 runs a DEPLOY mission from planet 1002 to planet 1234 with 5 units of id 10
    Then table missions has a row where user_id=1 and type_code=DEPLOYED and target_planet=1234
    And user 1 received websocket event "unit_mission_change"

  Scenario: A second deploy to the same foreign planet merges into the existing DEPLOYED mission
    # covers: B40 (merge branch, no second DEPLOYED mission row created)
    # Note: a foreign DEPLOYED stack's obtained_units row keeps source_planet at the
    # original departure planet (ObtainedUnitBo.moveUnit's non-owned branch never
    # rewrites it, only target_planet/mission_id) — so parked location is asserted
    # via target_planet, not the "on planet" Then (which reads source_planet).
    Given user 1 has 3 units of id 11 on planet 1002
    When user 1 runs a DEPLOY mission from planet 1002 to planet 1234 with 5 units of id 10
    And user 1 runs a DEPLOY mission from planet 1002 to planet 1234 with 3 units of id 11
    Then table obtained_units has a row where user_id=1 and unit_id=10 and target_planet=1234 and count=5
    And table obtained_units has a row where user_id=1 and unit_id=11 and target_planet=1234 and count=3
    And table missions has 1 row where user_id=1 and type_code=DEPLOYED and target_planet=1234

  Scenario: Redeploying an emptied DEPLOYED stack does not crash registration
    # covers: B13 (registration-time orphan-DEPLOYED marking — the exact shape of the
    # historical Rust crash fixed in commit 83a0ab9a)
    When user 1 runs a DEPLOY mission from planet 1002 to planet 1234 with 5 units of id 10
    And user 1 runs a DEPLOY mission from planet 1234 to planet 1002 with 5 units of id 10
    Then user 1 has 5 units of id 10 on planet 1002
    And table missions has a row where user_id=1 and type_code=DEPLOYED and target_planet=1234 and resolved=1

  Scenario: Deploying to the same planet you're already on is rejected
    # covers: B23
    When user 1 attempts a DEPLOY mission from planet 1002 to planet 1002 with 5 units of id 10
    Then the request is rejected with HTTP status 400

  Scenario: Deploy is refused entirely while the DEPLOYMENT_CONFIG kill-switch is DISALLOWED
    # covers: B4
    Given configuration "DEPLOYMENT_CONFIG" is "DISALLOWED"
    When user 1 attempts a DEPLOY mission from planet 1002 to planet 1234 with 5 units of id 10
    Then the request is rejected with HTTP status 400

  Scenario: Sending units the user doesn't hold on the source planet is rejected
    # covers: B9
    When user 1 attempts a DEPLOY mission from planet 1002 to planet 1234 with 5 units of id 11
    # Java raw-500s here (unhandled servlet error, observed live)
    Then the request is rejected with HTTP status 500

  Scenario: Establishing a base on a planet that changed owner mid-flight returns the survivors home
    # covers: B30, B43, B45, B46, B47
    Given planet 1234 is owned by user 2
    When user 1 runs an ESTABLISH_BASE mission from planet 1002 to planet 1234 with 5 units of id 10
    Then planet 1234 is owned by user 2
    And table missions has a row where user_id=1 and type_code=RETURN_MISSION and target_planet=1234
    And table obtained_units has a row where user_id=1 and unit_id=10 and mission_id is not null
    When the RETURN_MISSION mission of user 1 completes
    Then user 1 has 5 units of id 10 on planet 1002
    And table obtained_units has a row where user_id=1 and unit_id=10 and mission_id is null
    And user 1 received websocket event "unit_mission_change"

  Scenario: Cancelling a mission auto-registers a return mission
    # covers: B43, B44
    When user 1 runs an ESTABLISH_BASE mission from planet 1002 to planet 1234 with 5 units of id 10
    And user 1 cancels their latest mission
    Then the request succeeded
    And planet 1234 is owned by user 1
    And table missions has a row where user_id=1 and type_code=RETURN_MISSION and target_planet=1234

  Scenario: Conquering your own planet is rejected
    # covers: B21 (CONQUEST — reject own planet)
    When user 1 attempts a CONQUEST mission from planet 1002 to planet 1003 with 5 units of id 10
    Then the request is rejected with HTTP status 400
    And planet 1003 is owned by user 1

  Scenario: Conquering another player's home planet is rejected
    # covers: B22 (CONQUEST — reject home planet)
    Given user 1 has explored planet 1004
    When user 1 attempts a CONQUEST mission from planet 1002 to planet 1004 with 5 units of id 10
    Then the request is rejected with HTTP status 400
    And planet 1004 is owned by user 2

  Scenario: Sending a mission to an unexplored planet is rejected
    # covers: B3
    Given user 2 has 5 units of id 10 on planet 1004
    When user 2 attempts an ESTABLISH_BASE mission from planet 1004 to planet 1234 with 5 units of id 10
    Then the request is rejected with HTTP status 400
    And planet 1234 has no owner
