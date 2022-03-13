import { Planet } from '../pojos/planet.pojo';
import { RunningMission } from './running-mission.type';
import { Unit } from './unit.type';

/**
 * Represents an unit build running mission
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface UnitBuildRunningMission extends RunningMission {
    unit: Unit;
    count: number;
    sourcePlanet?: Planet;
}
