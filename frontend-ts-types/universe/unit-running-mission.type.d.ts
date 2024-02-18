import { User } from '../core';
import { Planet } from './planet.type';
import { ObtainedUnit } from './obtained-unit.type';
import { RunningMission } from './running-mission.type';

/**
 * Represents a running unit involved missions
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
export interface UnitRunningMission<U extends User = User> extends RunningMission {
    involvedUnits: ObtainedUnit[];
    invisible: boolean;
    sourcePlanet?: Planet;
    targetPlanet?: Planet;
    user?: U;
    wantedTime?: number;
}
