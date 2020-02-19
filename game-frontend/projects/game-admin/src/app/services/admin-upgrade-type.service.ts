import { Injectable } from '@angular/core';

import { AbstractCrudService, UpgradeType, UniverseGameService, CrudServiceAuthControl } from '@owge/universe';
import { validContext } from '@owge/core';

/**
 * admin service for managing upgrade types
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable()
export class AdminUpgradeTypeService extends AbstractCrudService<UpgradeType> {
    public constructor(protected _universeGameService: UniverseGameService) {
        super(_universeGameService);
    }

    protected _getEntity(): string {
        return 'upgrade_type';
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
