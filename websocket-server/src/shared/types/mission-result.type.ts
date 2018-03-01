import { User } from './user.type';

export type missionType = 'mission_explore';

/**
 * Represents a mission result to send to the client
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface MissionResult
 */
export interface MissionResult {
    user: User;

    /**
     * Content of the mission report
     *
     * @type {string}
     * @memberof MissionResult
     */
    content: any;
}
