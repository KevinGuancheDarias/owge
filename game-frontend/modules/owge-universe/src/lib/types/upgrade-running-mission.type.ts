import { RunningMission } from './running-mission.type';
import { Upgrade } from './upgrade.type';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface UpgradeRunningMission extends RunningMission {
    upgrade: Upgrade;
    level: number;
}
