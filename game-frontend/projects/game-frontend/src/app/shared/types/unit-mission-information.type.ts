import { SelectedUnit } from '@owge/universe';

/**
 * Represents the JSON commonly used to send unit missions (explore, gather... deploy . . . . ....)
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface UnitMissionInformation
 */
export interface UnitMissionInformation {
    sourcePlanetId: number;
    targetPlanetId: number;
    involvedUnits: SelectedUnit[];
}
