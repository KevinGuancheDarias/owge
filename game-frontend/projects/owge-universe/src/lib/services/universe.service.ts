import { UniverseStorage } from '../storages/universe.storage';
import { Universe } from '../types/universe.type';
import { Injectable } from '@angular/core';

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
    public constructor(private _universeStorage: UniverseStorage) { }

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
}
