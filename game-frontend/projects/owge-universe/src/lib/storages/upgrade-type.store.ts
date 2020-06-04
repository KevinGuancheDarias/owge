import { Subject, ReplaySubject } from 'rxjs';
import { UpgradeType } from '../types/upgrade-type.type';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export class UpgradeTypeStore {
    public readonly available: Subject<UpgradeType[]> = new ReplaySubject(1);
}
