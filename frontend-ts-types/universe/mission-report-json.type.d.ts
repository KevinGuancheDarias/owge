import { ObtainedUnit } from './obtained-unit.type';
import { User } from '../core';
import { Planet } from './planet.type';

/**
 * Represents the JSON content of a mission report
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
export interface MissionReportJson {
    id: number;
    involvedUnits: ObtainedUnit[];
    senderUser: User;
    sourcePlanet?: Planet;
    targetPlanet?: Planet;
}
