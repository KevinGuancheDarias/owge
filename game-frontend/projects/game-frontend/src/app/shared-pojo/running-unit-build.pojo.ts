import { AbstractRunningMissionPojo } from './abstract-running-mission.pojo';
import { Unit } from '@owge/universe';

export class RunningUnitPojo extends AbstractRunningMissionPojo {
    public unit: Unit;
    public count: number;
}
