import { UniverseStorage } from '../storages/universe.storage';
import { LoginSessionService } from '../../../login-session/login-session.service';
import { Universe } from '../../../shared-pojo/universe.pojo';
import { Injectable } from '@angular/core';

/**
 * Has methods related to universe handling
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.3
 * @export
 * @class UniverseService
 */
@Injectable()
export class UniverseService {
    public constructor(private _universeStorage: UniverseStorage) { }

    /**
     * Initializes the service Data
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.3
     * @memberof UniverseService
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
     * @returns {Universe}
     * @memberof UniverseService
     */
    public getSelectedUniverse(): Universe {
        return JSON.parse(sessionStorage.getItem(LoginSessionService.LOCAL_STORAGE_SELECTED_UNIVERSE));
    }
}
