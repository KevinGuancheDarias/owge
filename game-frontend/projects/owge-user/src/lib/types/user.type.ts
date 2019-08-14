import { Faction } from '../../../shared-pojo/faction.pojo';
import { PlanetPojo } from '../../../shared-pojo/planet.pojo';
import { Alliance } from '../../alliance/types/alliance.type';

/**
 * Better way to represent user details
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @interface User
 */
export interface User {
    id: number;
    username: string;
    email: string;
    password: string;
    activated: boolean;
    creationDate: Date;
    lastLogin: Date;
    firstName: string;
    lastName: string;
    notifications: boolean;

    factionDto: Faction;
    homePlanetDto: PlanetPojo;

    consumedEnergy: number;
    maxEnergy: number;
    primaryResource: number;
    primaryResourceGenerationPerSecond: number;
    secondaryResource: number;
    secondaryResourceGenerationPerSecond: number;

    computedPrimaryResourceGenerationPerSecond: number;
    computedSecondaryResourceGenerationPerSecond: number;

    alliance: Alliance;
}
