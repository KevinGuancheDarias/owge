import { Injectable } from '@angular/core';

import { AbstractCrudService, UniverseGameService, CrudServiceAuthControl, Upgrade } from '@owge/universe';
import { Faction } from '@owge/faction';
import { validContext } from '@owge/core';


/**
 * Has methods related with the CRUD of the Upgrade
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable()
export class AdminUpgradeService extends AbstractCrudService<Upgrade> {

    public constructor(protected _universeGameService: UniverseGameService) {
        super(_universeGameService);
    }

    protected _getEntity(): string {
        return 'upgrade';
    }

    protected _getContextPathPrefix(): validContext {
        return 'admin';
    }
    protected _getAuthConfiguration(): CrudServiceAuthControl {
        return {
            findAll: true,
            findById: true
        };
    }
}
