Feature: Planet list — player bookmarks
  Covers PlanetListRestService/PlanetListBo myAdd/myDelete and the
  planet_user_list_change push both emit. (The third emitter,
  emitByChangedPlanet on ownership changes, is already exercised by the
  conquest/establish-base scenarios.)

  Background:
    Given the standard test universe
    And user 1 has explored planet 1234

  Scenario: Adding a planet to the list stores it and pushes the new list
    When user 1 adds planet 1234 to their planet list as "watchtower"
    Then the request succeeded
    And table planet_list has a row where user_id=1 and planet_id=1234
    And user 1 received websocket event "planet_user_list_change"

  Scenario: Removing a planet from the list deletes the row and pushes again
    When user 1 adds planet 1234 to their planet list as "watchtower"
    And user 1 removes planet 1234 from their planet list
    Then the request succeeded
    And table planet_list has no row where user_id=1 and planet_id=1234
