import { Upgrade } from './upgrade.pojo';
import { AbstractRunningMissionPojo } from './abstract-running-mission.pojo';

export class RunningUpgrade extends AbstractRunningMissionPojo {
    public upgrade: Upgrade;
    public level: number;
}
