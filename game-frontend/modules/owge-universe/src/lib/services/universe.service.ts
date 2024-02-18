import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';


import { CoreHttpService, Config } from '@owge/core';

import { UniverseStorage } from '../storages/universe.storage';
import { Universe } from '@owge/types/universe';
import { Title } from '@angular/platform-browser';

/**
 * Has methods related to universe handling
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.3
 * @export
 */
@Injectable()
export class UniverseService {
    public static readonly LOCAL_STORAGE_SELECTED_UNIVERSE = 'owge_universe';
    public constructor(
        private _universeStorage: UniverseStorage,
        private _coreHttpService: CoreHttpService,
        private _titleService: Title
    ) { }

    /**
     * Finds the oficial servers
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @returns
     */
    public findOfficials(): Observable<Universe[]> {
        return this._coreHttpService.get(`${Config.accountServerUrl}/universe/findOfficials`);
    }

    /**
     * Initializes the service Data
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.3
     */
    public init(): void {
        const universe: Universe = this.getSelectedUniverse();
        if (universe) {
            this._universeStorage.currentUniverse.next(universe);
        }
    }

    /**
     * Returns the current universe information
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.3
     */
    public getSelectedUniverse(): Universe {
        return JSON.parse(sessionStorage.getItem(UniverseService.LOCAL_STORAGE_SELECTED_UNIVERSE));
    }

    /**
     * Defines the selected Universe
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param universe
     * @memberof UniverseService
     */
    public setSelectedUniverse(universe: Universe): void {
        this._universeStorage.currentUniverse.next(universe);
        sessionStorage.setItem(UniverseService.LOCAL_STORAGE_SELECTED_UNIVERSE, JSON.stringify(universe));
        this._titleService.setTitle(`OWGE :: ${universe.name}`);
    }
}
