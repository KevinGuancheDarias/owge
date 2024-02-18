import { Subject, ReplaySubject } from 'rxjs';
import { PlanetListItem } from '@owge/types/galaxy';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export class PlanetListStore {
    public readonly list: Subject<PlanetListItem[]> = new ReplaySubject(1);
}
