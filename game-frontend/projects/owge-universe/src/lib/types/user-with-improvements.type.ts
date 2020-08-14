import { User } from '@owge/core';
import { Improvement } from './improvement.type';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface UserWithImprovements extends User {
    improvements?: Improvement;
}
