import { Alliance } from './alliance.type';
import { UserWithAlliance } from './user-with-alliance.type';

/**
 * Represents a join request
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
export interface AllianceJoinRequest {
    id: number;

    /**
     * The alliance requested
     *
     */
    alliance: Alliance;

    /**
     * The user requesting an alliance
     *
     */
    user: UserWithAlliance;
}
