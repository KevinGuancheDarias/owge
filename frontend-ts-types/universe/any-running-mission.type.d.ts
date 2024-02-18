import { UnitBuildRunningMission } from './unit-build-running-mission.type';
import { UnitRunningMission } from './unit-running-mission.type';
import { UpgradeRunningMission } from './upgrade-running-mission.type';

export interface AnyRunningMission extends UnitBuildRunningMission, UnitRunningMission, UpgradeRunningMission {

}
