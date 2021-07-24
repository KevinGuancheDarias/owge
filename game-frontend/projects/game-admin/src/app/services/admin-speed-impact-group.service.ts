import { Injectable } from '@angular/core';
import { SpeedImpactGroup, validContext } from '@owge/core';
import {
    AbstractCrudService, CrudConfig, CrudServiceAuthControl, UniverseGameService, WithRequirementsCrudMixin
} from '@owge/universe';
import { WidgetFilter } from '@owge/core';
import { take } from 'rxjs/operators';
import { mix } from 'ts-mixer';

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

    public async buildFilter(): Promise<WidgetFilter<SpeedImpactGroup>> {
        return {
            name: 'FILTER.BY_SPEED_GROUP',
            data: await this.findAll().pipe(take(1)).toPromise(),
            filterAction: async (input: { speedImpactGroup: SpeedImpactGroup}, selected) => input.speedImpactGroup.id === selected.id
        };
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
