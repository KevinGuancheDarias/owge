import { SelectedUnit } from './selected-unit.type';

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
