import { Injectable } from '@angular/core';

import { AbstractCrudService, UnitType, UniverseGameService, CrudServiceAuthControl } from '@owge/universe';
import { validContext } from '@owge/core';

/**
 * admin service for managing unit types
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable()
export class AdminUnitTypeService extends AbstractCrudService<UnitType> {
    public constructor(protected _universeGameService: UniverseGameService) {
        super(_universeGameService);
    }

    protected _getEntity(): string {
        return 'unit_type';
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
