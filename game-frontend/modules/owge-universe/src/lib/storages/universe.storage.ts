import { ReplaySubject } from 'rxjs';
import { Injectable } from '@angular/core';

import { Universe } from '@owge/types/universe';

/**
 * Stores universe information <b>including selected universe</b>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
@Injectable()
export class UniverseStorage {

    /**
     * Current selected universe
     *
     * @since 0.7.0
     */
    public readonly currentUniverse: ReplaySubject<Universe> = new ReplaySubject(1);
}
