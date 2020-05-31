import { ObtainedUnit } from './obtained-unit.type';
import { User } from '@owge/core';
import { Planet } from '@owge/galaxy';

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
