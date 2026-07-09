Feature: Special-location unlocks
  Acquiring a planet that carries a special location must grant the relations
  gated by HAVE_SPECIAL_LOCATION on that special location; losing the planet
  must revoke them. Reference: Java PlanetBo.definePlanetAsOwnedBy /
  doLeavePlanet + ConquestMissionProcessor; see
  docs/BUG-SPECIAL-LOCATION-UNLOCK.md.

  Background:
    Given the standard test universe
    And planet 1234 has special location 500 and no owner
    And unit 9100 exists gated by requirement HAVE_SPECIAL_LOCATION with second value 500
    And time special 900 exists gated by requirement HAVE_SPECIAL_LOCATION with second value 500
    And user 1 has 5 units of id 10 on planet 1002
    And user 1 has explored planet 1234

  Scenario: Establish base grants the unlocks to the new owner
    When user 1 runs an ESTABLISH_BASE mission from planet 1002 to planet 1234 with 5 units of id 10
    Then planet 1234 is owned by user 1
    And table unlocked_relation has a row for user 1 and object UNIT reference 9100
    And table unlocked_relation has a row for user 1 and object TIME_SPECIAL reference 900
    And user 1 received websocket event "unit_unlocked_change" where some item has id 9100
    And user 1 received websocket event "time_special_unlocked_change" where some item has id 900

  Scenario: Conquest transfers the unlocks from old owner to new owner
    Given planet 1234 is owned by user 2
    And user 2 has an unlocked relation for object UNIT reference 9100
    And user 2 has an unlocked relation for object TIME_SPECIAL reference 900
    And user 2 has 1 unit of id 11 on planet 1234
    When user 1 runs a CONQUEST mission from planet 1002 to planet 1234 with 5 units of id 10
    Then planet 1234 is owned by user 1
    And table unlocked_relation has a row for user 1 and object UNIT reference 9100
    And table unlocked_relation has no row for user 2 and object UNIT reference 9100
    And user 1 received websocket event "unit_unlocked_change" where some item has id 9100
    And user 2 received websocket event "unit_unlocked_change" where no item has id 9100

  Scenario: Leaving the planet revokes the unlocks
    Given planet 1234 is owned by user 1
    And user 1 has an unlocked relation for object UNIT reference 9100
    When user 1 leaves planet 1234
    Then planet 1234 has no owner
    And table unlocked_relation has no row for user 1 and object UNIT reference 9100
    And user 1 received websocket event "unit_unlocked_change" where no item has id 9100
