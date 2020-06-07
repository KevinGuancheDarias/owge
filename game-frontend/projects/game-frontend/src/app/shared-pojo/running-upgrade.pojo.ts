import { Upgrade } from './upgrade.pojo';
import { AbstractRunningMissionPojo } from './abstract-running-mission.pojo';


/**
 *
 * @deprecated As of 0.9.0 it's better to use @owge/universe version
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 *
 */
export class RunningUpgrade extends AbstractRunningMissionPojo {
    public upgrade: Upgrade;
    public level: number;
}
