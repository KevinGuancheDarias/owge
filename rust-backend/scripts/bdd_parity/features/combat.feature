Feature: Attack and counterattack combat
  A plain ATTACK mission (combat outside the conquest wrapper) and the
  COUNTERATTACK variant (same processor; registration additionally requires
  the TARGET planet to belong to the sender — you counterattack enemy units
  parked on YOUR planet). Combat math runs under ATTACK_DETERMINISTIC_RNG
  (seed = mission id, identical across passes after the baseline restore), so
  exact unit counts are enforced by the TABLE parity diff — the spec Thens
  stay loose on purpose.

  Background:
    Given the standard test universe
    And user 1 has 10 units of id 10 on planet 1002
    And user 2 has 2 units of id 10 on planet 1004
    And user 1 has explored planet 1004

  Scenario: An attack mission wipes the defenders and the survivors return home
    When user 1 runs an ATTACK mission from planet 1002 to planet 1004 with 10 units of id 10
    Then table obtained_units has no row where user_id=2 and unit_id=10 and source_planet=1004
    And table missions has a row where user_id=1 and type_code=RETURN_MISSION
    And user 1 received websocket event "mission_report_new"
    And user 2 received websocket event "mission_report_new"
    And user 2 received websocket event "unit_obtained_change"
    When the RETURN_MISSION mission of user 1 completes
    Then table obtained_units has a row where user_id=1 and unit_id=10 and source_planet=1002 and mission_id is null

  Scenario: Counterattacking your own planet fights the enemy units parked there
    Given user 2 has explored planet 1003
    When user 2 runs a DEPLOY mission from planet 1004 to planet 1003 with 2 units of id 10
    And user 1 runs a COUNTERATTACK mission from planet 1002 to planet 1003 with 10 units of id 10
    Then table obtained_units has no row where user_id=2 and unit_id=10 and target_planet=1003
    And table missions has a row where user_id=1 and type_code=RETURN_MISSION
    And user 1 received websocket event "mission_report_new"
    When the RETURN_MISSION mission of user 1 completes
    Then table obtained_units has a row where user_id=1 and unit_id=10 and source_planet=1002 and mission_id is null

  Scenario: Counterattacking a planet you do not own is rejected
    When user 1 attempts a COUNTERATTACK mission from planet 1002 to planet 1234 with 5 units of id 10
    Then the request is rejected with HTTP status 400
