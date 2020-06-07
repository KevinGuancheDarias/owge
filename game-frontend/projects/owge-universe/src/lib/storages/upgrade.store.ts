import { Subject, ReplaySubject } from 'rxjs';
import { ObtainedUpgrade } from '../types/obtained-upgrade.type';
import { UpgradeRunningMission } from '../types/upgrade-running-mission.type';

/**
 * Stores the current synced status of anything upgrade related
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export class UpgradeStore {
    public obtained: Subject<ObtainedUpgrade[]> = new ReplaySubject(1);
    public runningLevelUpMission: Subject<UpgradeRunningMission> = new ReplaySubject(1);
}
