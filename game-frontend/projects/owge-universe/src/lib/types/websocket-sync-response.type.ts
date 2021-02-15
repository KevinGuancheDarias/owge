
/**
 * Represents the response from the server
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.6
 * @export
 */
export interface WebsocketSyncResponse {
    enemy_mission_change?: any;
    mission_report_change?: any;
    missions_count_change?: any;
    obtained_upgrades_change?: any;
    planet_owned_change?: any;
    planet_user_list_change?: any;
    running_upgrade_change?: any;
    speed_impact_group_unlocked_change?: any;
    time_special_change?: any;
    tutorial_entries_change?: any;
    twitch_state_change?: any;
    unit_mission_change?: any;
    unit_build_mission_change?: any;
    unit_obtained_change?: any;
    unit_requirements_change?: any;
    unit_type_change?: any;
    unit_unlocked_change?: any;
    upgrade_types_change?: any;
    user_data_change?: any;
    visited_tutorial_entry_change?: any;
    system_message_change?: any;
}
