Feature: Upgrade level-up registration, completion, and cancellation
  Registering a LEVEL_UP mission deducts the geometrically-grown
  (level_effect-compounded) resource cost at registration and creates a
  missions row with no source/target planet; completing it bumps
  obtained_upgrades.level to the target level and hard-deletes the mission
  (LEVEL_UP never sets missions.resolved=1, like BUILD_UNIT); cancelling
  refunds the cost in full and hard-deletes the mission. Only one LEVEL_UP
  mission may run per user at a time, regardless of which upgrade it targets.
  Reference: business/MissionBo.java registerLevelUpAnUpgrade (:112-163) /
  processLevelUpAnUpgrade (:179-203) / cancelUpgradeMission (:299-303);
  business/UpgradeBo.java calculateRequirementsAreMet (:103-120); see
  inventories/upgrades.md.

  Background:
    Given the standard test universe

  Scenario: Registering a level-up deducts resources and creates the mission
    # covers: B1
    Given user 1 has 490 primary resource and 330 secondary resource
    And user 1 has obtained upgrade 1 at level 0 available
    When user 1 registers a LEVEL_UP mission for upgrade 1
    Then the request succeeded
    And table missions has a row where user_id=1 and type_code=LEVEL_UP
    And user 1 has primary resource 0 and secondary resource 0
    And user 1 received websocket event "running_upgrade_change"
    And user 1 received websocket event "missions_count_change"
    And user 1 received websocket event "user_data_change"

  Scenario: Registration is rejected when the user lacks the resources to pay for it
    # covers: B1
    Given user 1 has 489 primary resource and 330 secondary resource
    And user 1 has obtained upgrade 1 at level 0 available
    When user 1 attempts to register a LEVEL_UP mission for upgrade 1
    Then the request is rejected with error containing "No enough resources!"
    And table missions has no row where user_id=1 and type_code=LEVEL_UP
    And user 1 has primary resource 489 and secondary resource 330

  Scenario: Registration is rejected when the upgrade is not available
    # covers: B10
    Given user 1 has 490 primary resource and 330 secondary resource
    And user 1 has obtained upgrade 1 at level 0 unavailable
    When user 1 attempts to register a LEVEL_UP mission for upgrade 1
    Then the request is rejected with error containing "Can't register mission, of type LEVEL_UP, when upgrade is not available!"
    And table missions has no row where user_id=1 and type_code=LEVEL_UP
    And user 1 has primary resource 490 and secondary resource 330

  Scenario: A second level-up cannot be registered while one is already running
    # covers: B4
    Given user 1 has 490 primary resource and 330 secondary resource
    And user 1 has obtained upgrade 1 at level 0 available
    When user 1 registers a LEVEL_UP mission for upgrade 1
    Then the request succeeded
    When user 1 attempts to register a LEVEL_UP mission for upgrade 1
    Then the request is rejected with error containing "There is already an upgrade going"

  Scenario: Completing a level-up bumps the obtained-upgrade level
    # covers: B2
    Given user 1 has 490 primary resource and 330 secondary resource
    And user 1 has obtained upgrade 1 at level 0 available
    When user 1 registers a LEVEL_UP mission for upgrade 1
    And the LEVEL_UP mission of user 1 completes
    Then table obtained_upgrades has a row where user_id=1 and upgrade_id=1 and level=1
    And table missions has no row where user_id=1 and type_code=LEVEL_UP
    And user 1 received websocket event "obtained_upgrades_change"
    And user 1 received websocket event "running_upgrade_change"
    And user 1 received websocket event "missions_count_change"

  Scenario: Cancelling a running level-up refunds resources and clears the mission
    # covers: B3
    Given user 1 has 490 primary resource and 330 secondary resource
    And user 1 has obtained upgrade 1 at level 0 available
    When user 1 registers a LEVEL_UP mission for upgrade 1
    Then the request succeeded
    When user 1 cancels the running upgrade mission
    Then user 1 has primary resource 490 and secondary resource 330
    And table missions has no row where user_id=1 and type_code=LEVEL_UP
    And user 1 received websocket event "running_upgrade_change"
    And user 1 received websocket event "unit_type_change"
    And user 1 received websocket event "missions_count_change"

  Scenario: Cancelling with no running level-up mission is rejected
    # covers: B3
    Given user 1 has obtained upgrade 1 at level 0 available
    When user 1 cancels the running upgrade mission
    Then the request is rejected with HTTP status 404

  Scenario: ZERO_UPGRADE_TIME collapses the required time to 3 seconds
    # covers: B1
    Given configuration "ZERO_UPGRADE_TIME" is "TRUE"
    And user 1 has 490 primary resource and 330 secondary resource
    And user 1 has obtained upgrade 1 at level 0 available
    When user 1 registers a LEVEL_UP mission for upgrade 1
    Then table missions has a row where user_id=1 and type_code=LEVEL_UP and required_time=3
