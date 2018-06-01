import { PlanetPojo } from './planet.pojo';
import { UnitPojo } from './unit.pojo';

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
    public unit: UnitPojo;
    public count: number;
    public sourcePlanet?: PlanetPojo;
    public targetPlnet?: PlanetPojo;
    public mission?: any;
    public expiration?: Date;
}
