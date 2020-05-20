import { RunningMission } from './running-mission.type';
import { Planet } from '@owge/galaxy';
import { User } from '@owge/core';
import { ObtainedUnit } from './obtained-unit.type';

/**
 * Represents a running unit involved missions
 *
 * @deprecated As of 0.9.0 this is in @owge/universe
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
export interface UnitRunningMission extends RunningMission {
    involvedUnits: ObtainedUnit[];
    sourcePlanet?: Planet;
    targetPlanet?: Planet;
    user?: User;
}
