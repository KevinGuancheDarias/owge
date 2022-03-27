import { Planet } from '../pojos/planet.pojo';
import { ÇbtainedUnitTemporalInformation } from './obtained-unit-temporal-information.type';
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
    username: string;
    sourcePlanet?: Planet;
    targetPlanet?: Planet;
    mission?: any;
    temporalInformation?: ÇbtainedUnitTemporalInformation;
}
