import { Planet } from '@owge/galaxy';
import { Unit } from './unit.type';

/**
 * Represents an obtained unit
 *
 * @todo Change mission by mission pojo
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
export interface ObtainedUnit {
    id: number;
    unit: Unit;
    count: number;
    sourcePlanet?: Planet;
    targetPlanet?: Planet;
    mission?: any;
    expiration?: Date;
}
