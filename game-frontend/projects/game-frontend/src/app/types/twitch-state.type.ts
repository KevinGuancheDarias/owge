
/**
 * Controls the twitch display state
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.1
 * @export
 */
export interface TwitchState {

    /**
     * If has to display the iframe
     *
     * @since 0.9.1
     */
    hasToDisplay: boolean;

    /**
     * If the tab is the primary (the one enabling the Twitch, we should unmute it)
     *
     * @since 0.9.1
     */
    isPrimary: boolean;
}
