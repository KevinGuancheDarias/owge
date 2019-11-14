import { Subject, ReplaySubject } from 'rxjs';

/**
 * Stores missions information
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export class MissionStore {
    public missionsCount: Subject<number> = new ReplaySubject();
    public maxMissions: Subject<number> = new ReplaySubject();
}
