import { ReplaySubject } from 'rxjs';
import { Universe } from '../../../shared-pojo/universe.pojo';

/**
 * Stores universe information <b>including selected universe</b>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @class UniverseStorage
 */
export class UniverseStorage {

    /**
     * Current selected universe
     *
     * @since 0.7.0
     * @type {ReplaySubject<Universe>}
     * @memberof UniverseStorage
     */
    public readonly currentUniverse: ReplaySubject<Universe> = new ReplaySubject(1);
}
