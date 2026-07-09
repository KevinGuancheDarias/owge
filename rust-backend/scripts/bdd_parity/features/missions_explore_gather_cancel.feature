Feature: Explore, gather, and mission cancel
  Reference: business/mission/processor/ExploreMissionProcessor.java,
  business/mission/processor/GatherMissionProcessor.java,
  business/UnitMissionBo.java#myCancelMission; inventory
  rust-backend/scripts/bdd_parity/inventories/missions-explore-gather-cancel.md.

  Background:
    Given the standard test universe
    And user 1 has 5 units of id 10 on planet 1002

  # covers: B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11, B12, B13, B14, B15, B21
  Scenario: Explore mission discovers an unowned, unexplored planet
    When user 1 runs an EXPLORE mission from planet 1002 to planet 1234 with 5 units of id 10
    Then table missions has a row where user_id=1 and type_code=EXPLORE and resolved=1
    And table explored_planets has a row where user=1 and planet=1234
    And table missions has a row where user_id=1 and type_code=RETURN_MISSION and related_mission is not null
    And user 1 received websocket event "planet_explored_event"
    And user 1 received websocket event "mission_report_new"
    And user 1 received websocket event "unit_mission_change"

  # covers: B1, B2, B3, B6, B8, B9, B10, B11, B16, B17, B18, B19, B21
  Scenario: Gather mission resolves and produces a report
    Given user 1 has explored planet 1234
    When user 1 runs a GATHER mission from planet 1002 to planet 1234 with 5 units of id 10
    Then table missions has a row where user_id=1 and type_code=GATHER and resolved=1
    And table mission_reports has a row where user_id=1
    And user 1 received websocket event "mission_gather_result"
    And user 1 received websocket event "mission_report_new"

  # covers: B1
  Scenario: Registering a mission beyond the concurrent mission slot limit is rejected
    Given user 1 has 3 units of id 11 on planet 1002
    When user 1 runs an EXPLORE mission from planet 1002 to planet 1234 with 5 units of id 10
    And user 1 attempts an EXPLORE mission from planet 1002 to planet 1234 with 3 units of id 11
    Then the request is rejected with error containing "I18N_ERR_MISSION_LIMIT_EXCEEDED"

  # covers: B2
  Scenario: Gathering from an unexplored target planet is rejected
    When user 1 attempts a GATHER mission from planet 1002 to planet 1004 with 5 units of id 10
    Then the request is rejected with error containing "target planet is not explored"

  # covers: B6
  Scenario: Sending more units than owned on the source planet is rejected
    When user 1 attempts an EXPLORE mission from planet 1002 to planet 1234 with 6 units of id 10
    Then the request is rejected with error containing "Can't not subtract because, obtainedUnit count is less than the amount to subtract"

  # covers: B22, B23, B25
  Scenario: Cancelling an already-resolved explore mission still succeeds and registers a second return mission
    When user 1 runs an EXPLORE mission from planet 1002 to planet 1234 with 5 units of id 10
    And user 1 cancels their latest mission
    Then table missions has 2 rows where user_id=1 and type_code=RETURN_MISSION
    And user 1 received websocket event "unit_mission_change"
