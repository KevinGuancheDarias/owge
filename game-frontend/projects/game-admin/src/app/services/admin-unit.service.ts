import { Injectable } from '@angular/core';
import {
    Unit,
    AbstractCrudService, WithRequirementsCrudMixin, WithImprovementsCrudMixin, CrudConfig, UniverseGameService, CrudServiceAuthControl
} from '@owge/universe';
import { validContext } from '@owge/core';
import { Mixin } from 'ts-mixer';

export interface AdminUnitService
    extends AbstractCrudService<Unit, number>, WithRequirementsCrudMixin<number>, WithImprovementsCrudMixin<number> { }


/**
 * The service to manage the units
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Injectable()
export class AdminUnitService extends AbstractCrudService<Unit, number> {
    protected _crudConfig: CrudConfig;

    public constructor(protected _universeGameService: UniverseGameService) {
        super(_universeGameService);
        this._crudConfig = this.getCrudConfig();
    }

    protected _getEntity(): string {
        return 'unit';
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
(<any>AdminUnitService) = Mixin(WithImprovementsCrudMixin, WithRequirementsCrudMixin, <any>AdminUnitService);
