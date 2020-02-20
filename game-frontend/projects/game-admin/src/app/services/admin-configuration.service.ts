import { Injectable } from '@angular/core';

import { AbstractCrudService, UniverseGameService, CrudServiceAuthControl } from '@owge/universe';
import { validContext } from '@owge/core';

import { Configuration } from '../types/configuration.type';



/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Injectable()
export class AdminConfigurationService extends AbstractCrudService<Configuration> {
    public constructor(protected _universeGameService: UniverseGameService) {
        super(_universeGameService);
    }

    protected _getEntity(): string {
        return 'configuration';
    }

    protected _getIdKey(): keyof Configuration {
        return 'name';
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
