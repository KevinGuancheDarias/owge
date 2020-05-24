import { AbstractRunningMissionPojo } from './abstract-running-mission.pojo';
import { Unit } from '@owge/universe';


/**
 *
 * @deprecated As of 0.9.0 it's in @owge/universe/types/UnitBuildRunningMission
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
export class RunningUnitPojo extends AbstractRunningMissionPojo {
    public unit: Unit;
    public count: number;
}
