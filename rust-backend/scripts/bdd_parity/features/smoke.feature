Feature: Harness smoke
  Phase-0 pipeline proof (BDD-PARITY-PLAN.md §8): no When step, so no backend
  is exercised — only DB seeding and DB assertion, run through the full
  runner (reset, both "backend" passes, layer-2 diff). All verdicts must be
  green by construction.

  Scenario: Seeded units are visible
    Given the standard test universe
    And user 1 has 5 units of id 10 on planet 1002
    Then user 1 has 5 units of id 10 on planet 1002
