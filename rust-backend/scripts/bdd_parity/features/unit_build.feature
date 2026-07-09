Feature: Unit build registration, completion, and cancellation
  Building a unit reserves its resource cost at mission REGISTRATION (not on
  completion) and creates an in-build obtained_units row with no planet;
  completing the BUILD_UNIT mission lands the units on the source planet and
  hard-deletes the mission row (BUILD_UNIT never sets missions.resolved=1);
  cancelling refunds the reserved cost in full and hard-deletes the in-build
  row. Reference: Java MissionBo.registerBuildUnit / processBuildUnit,
  MissionCancelBuildService.cancel; see inventories/unit-build.md.

  Background:
    Given the standard test universe
    And user 1 has an unlocked relation for object UNIT reference 10
    And user 1 has 0 units of id 10 on planet 1002

  Scenario: Registering a build deducts resources at registration; completion lands the units
    # covers: B1, B13, B13b, B15, B17, B18
    Given user 1 has 50000 primary resource and 40000 secondary resource
    When user 1 builds 5 units of id 10 on planet 1002
    # resources accrue per second; the deduction is asserted via the cost the
    # mission row recorded (5 x 9000/6000), not the live balance
    Then table missions has a row where user_id=1 and type_code=BUILD_UNIT and primary_resource=45000 and secondary_resource=30000
    # the event's items are build-mission DTOs (item.id = mission id, not the
    # unit id) — item-level introspection is left to the layer-2 ws diff
    And user 1 received websocket event "unit_build_mission_change"
    When the BUILD_UNIT mission of user 1 completes
    Then table missions has no row where user_id=1 and type_code=BUILD_UNIT
    And table obtained_units has a row where user_id=1 and unit_id=10 and count=5 and mission_id is null
    And user 1 has 5 units of id 10 on planet 1002
    And user 1 received websocket event "unit_obtained_change"

  Scenario: Cancelling a build refunds the reserved cost and removes the in-build stack
    # covers: B19, B21, B22
    Given user 1 has 50000 primary resource and 40000 secondary resource
    When user 1 builds 5 units of id 10 on planet 1002
    Then table missions has a row where user_id=1 and type_code=BUILD_UNIT and primary_resource=45000 and secondary_resource=30000
    When user 1 cancels their build mission on planet 1002
    # refund exactness is unassertable (per-second accrual); the deterministic
    # observables are the deleted mission + in-build stack and the events
    Then table obtained_units has no row where user_id=1 and unit_id=10 and mission_id is not null
    And table missions has no row where user_id=1 and type_code=BUILD_UNIT
    And user 1 received websocket event "unit_build_mission_change"
    And user 1 received websocket event "unit_type_change"

  Scenario: Build is rejected when the user lacks the resources to pay for it
    # covers: B8
    Given user 1 has 1 primary resource and 1 secondary resource
    When user 1 attempts to build 1 units of id 10 on planet 1002
    # D5 ruling: proper 400 + message is the contract
    Then the request is rejected with HTTP status 400
    And the request is rejected with error containing "No enough resources!"
    And table missions has no row where user_id=1 and type_code=BUILD_UNIT

  Scenario: Build is rejected when the unit is not unlocked for the user
    # covers: B4
    Given user 1 has no unlocked relation for object UNIT reference 11
    When user 1 attempts to build 1 units of id 11 on planet 1002
    # D5 ruling: proper 400 is the contract (message wording differs per
    # backend for this one — status-only assertion)
    Then the request is rejected with HTTP status 400
    And table missions has no row where user_id=1 and type_code=BUILD_UNIT
    And table unlocked_relation has no row for user 1 and object UNIT reference 11
