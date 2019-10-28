import { Injectable } from '@angular/core';
import { ReplaySubject } from 'rxjs';

import { SessionStore } from '@owge/core';

import { Planet } from '../pojos/planet.pojo';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable()
export class PlanetStore {
    public readonly selectedPlanet: ReplaySubject<Planet> = new ReplaySubject(1);

    public constructor(private _sessionStore: SessionStore) {
        this._sessionStore.addSubject('selectedPlanet', this.selectedPlanet);
    }
}
