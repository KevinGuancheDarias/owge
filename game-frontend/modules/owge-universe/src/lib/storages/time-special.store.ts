import { TimeSpecial } from '@owge/types/universe';
import { Subject, ReplaySubject } from 'rxjs';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export class TimeSpecialStore {
    public readonly unlocked: Subject<TimeSpecial[]> = new ReplaySubject(1);
}
