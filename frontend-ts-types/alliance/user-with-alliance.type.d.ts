import { Alliance } from './alliance.type';
import { User } from '../core';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.0.1
 * @export
 */
export interface UserWithAlliance extends User {
    alliance: Alliance;
}
