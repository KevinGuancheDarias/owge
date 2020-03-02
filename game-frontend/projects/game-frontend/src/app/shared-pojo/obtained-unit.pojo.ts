import { PlanetPojo } from './planet.pojo';
import { Unit } from '@owge/universe';

/**
 * Represents an obtained unit
 *
 * @todo Change mission by mission pojo
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @class ObtainedUnit
 */
export class ObtainedUnit {
    public id: number;
    public unit: Unit;
    public count: number;
    public sourcePlanet?: PlanetPojo;
    public targetPlanet?: PlanetPojo;
    public mission?: any;
    public expiration?: Date;
}
