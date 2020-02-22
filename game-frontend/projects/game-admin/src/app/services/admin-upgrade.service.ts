import { Injectable } from '@angular/core';
import { Mixin } from 'ts-mixer';

import {
    AbstractCrudService,
    UniverseGameService,
    CrudServiceAuthControl,
    Upgrade,
    WithRequirementsCrudMixin,
    WithImprovementsCrudMixin,
    CrudConfig
} from '@owge/universe';
import { validContext } from '@owge/core';

export interface AdminUpgradeService
    extends AbstractCrudService<Upgrade, number>, WithRequirementsCrudMixin<number>, WithImprovementsCrudMixin<number> { }

/**
 * Has methods related with the CRUD of the Upgrade
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable()
export class AdminUpgradeService extends AbstractCrudService<Upgrade, number> {

    protected _crudConfig: CrudConfig;

    public constructor(protected _universeGameService: UniverseGameService) {
        super(_universeGameService);
        this._crudConfig = this.getCrudConfig();
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
(<any>AdminUpgradeService) = Mixin(WithImprovementsCrudMixin, WithRequirementsCrudMixin, <any>AdminUpgradeService);
