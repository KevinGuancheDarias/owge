import { Injectable } from '@angular/core';
import {
    AbstractCrudService, WithRequirementsCrudMixin, CrudConfig, UniverseGameService, CrudServiceAuthControl
} from '@owge/universe';
import { mix } from 'ts-mixer';
import { validContext, SpeedImpactGroup } from '@owge/core';

export interface AdminSpeedImpactGroupService extends AbstractCrudService<SpeedImpactGroup>, WithRequirementsCrudMixin { }

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@mix(WithRequirementsCrudMixin)
@Injectable()
export class AdminSpeedImpactGroupService extends AbstractCrudService<SpeedImpactGroup> {
    protected _crudConfig: CrudConfig;

    public constructor(protected _universeGameService: UniverseGameService) {
        super(_universeGameService);
        this._crudConfig = this.getCrudConfig();
    }

    protected _getEntity(): string {
        return 'speed-impact-group';
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
