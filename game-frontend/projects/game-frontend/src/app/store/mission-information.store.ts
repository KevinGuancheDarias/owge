import { Injectable } from '@angular/core';
import { ReplaySubject } from 'rxjs';

import { Planet, ObtainedUnit } from '@owge/types/universe';

/**
 * Stores the information to display in the <i>MissionModalComponent</i>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @class MissionInformationStore
 */
@Injectable()
export class MissionInformationStore {


    /**
     * Will emit when a mission has been sent
     *
     * @type {ReplaySubject<void>}
     * @memberof MissionInformationStore
     */
    public readonly missionSent: ReplaySubject<void> = new ReplaySubject();

    /**
     * Planet used as <i>sourcePlanet</i> in the backend endpoint
     *
     * @type {ReplaySubject<Planet>}
     * @readonly
     * @memberof MissionInformationStore
     */
    public readonly originPlanet: ReplaySubject<Planet> = new ReplaySubject();


    /**
     * Planet to which the mission goes to
     *
     * @type {ReplaySubject<Planet>}
     * @readonly
     * @memberof MissionInformationStore
     */
    public readonly targetPlanet: ReplaySubject<Planet> = new ReplaySubject();


    /**
     * Units that should be selectable in the modal
     *
     * @type {ReplaySubject<ObtainedUnit[]>}
     * @readonly
     * @memberof MissionInformationStore
     */
    public readonly availableUnits: ReplaySubject<ObtainedUnit[]> = new ReplaySubject();
}
