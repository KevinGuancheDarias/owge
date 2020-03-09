import { Injectable } from '@angular/core';
import {
    AbstractCrudService,
    SpecialLocation,
    WithImprovementsCrudMixin,
    UniverseGameService,
    CrudServiceAuthControl,
    CrudConfig
} from '@owge/universe';
import { validContext } from '@owge/core';
import { Mixin } from 'ts-mixer';

export interface AdminSpecialLocationService
    extends AbstractCrudService<SpecialLocation, number>, WithImprovementsCrudMixin<number> { }

/**
 * Admin special location service for crud operations
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Injectable()
export class AdminSpecialLocationService extends AbstractCrudService<SpecialLocation, number> {

    protected _crudConfig: CrudConfig;

    public constructor(protected _universeGameService: UniverseGameService) {
        super(_universeGameService);
        this._crudConfig = this.getCrudConfig();
    }

    protected _getEntity(): string {
        return 'special-location';
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
(<any>AdminSpecialLocationService) = Mixin(WithImprovementsCrudMixin, <any>AdminSpecialLocationService);

