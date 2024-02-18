import { PlanetPojo } from './planet.pojo';
import { Unit } from '@owge/types/universe';

/**
 * Represents an obtained unit
 *
 * @deprecated As Of 0.9.0 use the version in @owge/universe
 * @todo Change mission by mission pojo
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
export interface ObtainedUnit {
    id: number;
    unit: Unit;
    count: number;
    sourcePlanet?: PlanetPojo;
    targetPlanet?: PlanetPojo;
    mission?: any;
    expiration?: Date;
}
