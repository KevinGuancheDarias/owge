import { RunningMission } from './running-mission.type';
import { User } from '@owge/core';
import { ObtainedUnit } from './obtained-unit.type';
import { Planet } from '../pojos/planet.pojo';

/**
 * Represents a running unit involved missions
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
export interface UnitRunningMission extends RunningMission {
    involvedUnits: ObtainedUnit[];
    sourcePlanet?: Planet;
    targetPlanet?: Planet;
    user?: User;
}
