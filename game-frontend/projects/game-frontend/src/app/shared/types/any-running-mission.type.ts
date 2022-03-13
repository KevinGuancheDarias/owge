import { RunningUnitPojo } from '../../shared-pojo/running-unit-build.pojo';
import { RunningUpgrade } from '../../shared-pojo/running-upgrade.pojo';
import { UnitRunningMission } from './unit-running-mission.type';

export interface AnyRunningMission extends RunningUnitPojo, RunningUpgrade, UnitRunningMission {
}
