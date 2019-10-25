import { Injectable } from '@angular/core';

import { AbstractCrudService, UniverseGameService, CrudServiceAuthControl } from '@owge/universe';
import { Faction } from '@owge/faction';
import { validContext } from '@owge/core';


/**
 * Has methods related with the CRUD of the Faction
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 * @class AdminFactionService
 * @extends {AbstractCrudService<Faction>}
 */
@Injectable()
export class AdminFactionService extends AbstractCrudService<Faction> {

    public constructor(protected _universeGameService: UniverseGameService) {
        super(_universeGameService);
    }

    protected _getEntity(): string {
        return 'faction';
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
