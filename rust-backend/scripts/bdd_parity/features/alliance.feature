Feature: Alliances — create, join, leave, delete
  Covers the alliance lifecycle exposed by AllianceRestService/AllianceBo:
  creation (owner auto-joins), the join-request/accept flow, member vs owner
  leave semantics, and full deletion. The delete-with-pending-requests
  scenario is the inventory's JAVA-SUSPECT: alliance_join_request.alliance_id
  has no ON DELETE CASCADE and Java's delete path never removes the pending
  requests, while the Rust port deletes them explicitly.

  Background:
    Given the standard test universe
    # the seed universe ships with the alliance feature switched off
    And configuration "DISABLED_FEATURE_ALLIANCE" is "FALSE"

  Scenario: Creating an alliance sets the creator as owner and member
    When user 1 creates an alliance named "BDD Alliance"
    Then the request succeeded
    And table alliances has a row where name=BDD Alliance and owner_id=1
    And table user_storage has a row where id=1 and alliance_id is not null

  Scenario: Creating a second alliance while already in one is rejected
    When user 1 creates an alliance named "First"
    And user 1 attempts to create an alliance named "Second"
    Then the request is rejected with HTTP status 400
    And the request is rejected with error containing "You already have an alliance"
    And table alliances has 1 row where owner_id=1

  Scenario: Joining an alliance via request and owner acceptance
    When user 1 creates an alliance named "BDD Alliance"
    And user 2 requests to join the alliance owned by user 1
    Then table alliance_join_request has a row where user_id=2
    When user 1 accepts the join request of user 2
    Then table user_storage has a row where id=2 and alliance_id is not null
    And table alliance_join_request has no row where user_id=2

  Scenario: A member can leave but the owner cannot leave their own alliance
    When user 1 creates an alliance named "BDD Alliance"
    And user 2 requests to join the alliance owned by user 1
    And user 1 accepts the join request of user 2
    And user 2 leaves their alliance
    Then table user_storage has a row where id=2 and alliance_id is null
    When user 1 attempts to leave their alliance
    Then the request is rejected with HTTP status 400
    And the request is rejected with error containing "You can't leave your own alliance"

  Scenario: Deleting the alliance clears members and pending join requests
    # JAVA-SUSPECT: no FK cascade on alliance_join_request and no explicit
    # cleanup in Java's AllianceBo.delete — a pending request at delete time
    # is expected to break the Java side until fixed
    When user 1 creates an alliance named "BDD Alliance"
    And user 2 requests to join the alliance owned by user 1
    And user 1 deletes their alliance
    Then the request succeeded
    And table alliances has no row where owner_id=1
    And table alliance_join_request has no row where user_id=2
    And table user_storage has a row where id=1 and alliance_id is null
