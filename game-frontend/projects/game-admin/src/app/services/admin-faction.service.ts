import { Injectable } from '@angular/core';

import {
    AbstractCrudService,
    UniverseGameService,
    CrudServiceAuthControl,
    WithRequirementsCrudMixin,
    WithImprovementsCrudMixin,
    CrudConfig
} from '@owge/universe';
import { Faction } from '@owge/faction';
import { validContext } from '@owge/core';
import { Mixin } from 'ts-mixer';


export interface AdminFactionService
    extends AbstractCrudService<Faction>, WithRequirementsCrudMixin<number>, WithImprovementsCrudMixin<number> { }

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

    protected _crudConfig: CrudConfig;

    public constructor(protected _universeGameService: UniverseGameService) {
        super(_universeGameService);
        this._crudConfig = this.getCrudConfig();
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
(<any>AdminFactionService) = Mixin(WithImprovementsCrudMixin, WithRequirementsCrudMixin, <any>AdminFactionService);
