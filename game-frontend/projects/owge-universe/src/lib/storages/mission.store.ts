import { Subject, ReplaySubject } from 'rxjs';
import { UnitRunningMission } from '../types/unit-running-mission.type';

/**
 * Stores missions information
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export class MissionStore {
    public missionsCount: Subject<number> = new ReplaySubject(1);
    public maxMissions: Subject<number> = new ReplaySubject(1);
    public myUnitMissions: Subject<UnitRunningMission<any>[]> = new ReplaySubject(1);
    public enemyUnitMissions: Subject<UnitRunningMission<any>[]> = new ReplaySubject(1);
}
