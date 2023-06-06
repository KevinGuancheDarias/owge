import { ReplaySubject, Subject } from 'rxjs';

import { SessionStore } from '@owge/core';

import { Planet } from '@owge/universe';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export class PlanetStore {
    public readonly selectedPlanet: Subject<Planet> = new ReplaySubject(1);
    public readonly ownedPlanetList: Subject<Planet[]> = new ReplaySubject(1);

    /**
     * If changed while was offline, the value will be null
     *
     * @since 0.9.0
     */
    public readonly exploredEvent: Subject<Planet> = new Subject;

    public constructor(private _sessionStore: SessionStore) {
        this._sessionStore.addSubject('selectedPlanet', this.selectedPlanet);
    }
}
