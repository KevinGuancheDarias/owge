import { Subject, ReplaySubject } from 'rxjs';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export class SpeedImpactGroupStore {
    public readonly unlockedIds: Subject<number[]> = new ReplaySubject(1);
}
