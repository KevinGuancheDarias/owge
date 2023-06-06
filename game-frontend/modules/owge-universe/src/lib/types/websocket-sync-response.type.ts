import { Instant } from "@owge/core";

export interface WebsocketSyncItem {
    data: any;
    lastSent: number;
}

/**
 * Represents the response from the server
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.6
 * @export
 */
export interface WebsocketSyncResponse {
    enemy_mission_change?: WebsocketSyncItem;
    mission_report_change?: WebsocketSyncItem;
    missions_count_change?: WebsocketSyncItem;
    obtained_upgrades_change?: WebsocketSyncItem;
    planet_owned_change?: WebsocketSyncItem;
    planet_user_list_change?: WebsocketSyncItem;
    running_upgrade_change?: WebsocketSyncItem;
    speed_impact_group_unlocked_change?: WebsocketSyncItem;
    time_special_change?: WebsocketSyncItem;
    tutorial_entries_change?: WebsocketSyncItem;
    twitch_state_change?: WebsocketSyncItem;
    unit_mission_change?: WebsocketSyncItem;
    unit_build_mission_change?: WebsocketSyncItem;
    unit_obtained_change?: WebsocketSyncItem;
    unit_requirements_change?: WebsocketSyncItem;
    unit_type_change?: WebsocketSyncItem;
    unit_unlocked_change?: WebsocketSyncItem;
    upgrade_types_change?: WebsocketSyncItem;
    user_data_change?: WebsocketSyncItem;
    visited_tutorial_entry_change?: WebsocketSyncItem;
    system_message_change?: WebsocketSyncItem;
}
