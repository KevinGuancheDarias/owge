import { User } from '@owge/core';

import { Faction } from './faction.type';

/**
 * User that has a faction
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface UserWithFaction extends User {
    faction: Faction;
}
