import { Alliance } from './alliance.type';
import { User } from '../../user/types/user.type';

/**
 * Represents a join request
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @interface AllianceJoinRequest
 */
export interface AllianceJoinRequest {
    id: number;


    /**
     * The alliance requested
     *
     * @type {Alliance}
     * @memberof AllianceJoinRequest
     */
    alliance: Alliance;


    /**
     * The user requesting an alliance
     *
     * @type {User}
     * @memberof AllianceJoinRequest
     */
    user: User;
}
