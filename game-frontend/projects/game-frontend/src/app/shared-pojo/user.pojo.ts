import { PlanetPojo } from './planet.pojo';
import { Faction } from './faction.pojo';

export class UserPojo {
    public id: number;
    public username: string;
    public email: string;
    public password: string;
    public activated: boolean;
    public creationDate: Date;
    public lastLogin: Date;
    public firstName: string;
    public lastName: string;
    public notifications: boolean;

    public factionDto: Faction;
    public homePlanetDto: PlanetPojo;

    public consumedEnergy: number;
    public maxEnergy: number;
    public primaryResource: number;
    public primaryResourceGenerationPerSecond: number;
    public secondaryResource: number;
    public secondaryResourceGenerationPerSecond: number;

    public computedPrimaryResourceGenerationPerSecond: number;
    public computedSecondaryResourceGenerationPerSecond: number;
}
