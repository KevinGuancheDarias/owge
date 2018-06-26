import { UnitPojo } from './unit.pojo';
import { AbstractRunningMissionPojo } from './abstract-running-mission.pojo';

export class RunningUnitPojo extends AbstractRunningMissionPojo {
    public unit: UnitPojo;
    public count: number;
}
