import { Injectable } from '@angular/core';
import { validContext } from '@owge/core';
import { AbstractCrudService, CrudServiceAuthControl, UniverseGameService, UpgradeType } from '@owge/universe';
import { WidgetFilter } from '@owge/core';
import { take } from 'rxjs/operators';


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

    public async buildFilter(): Promise<WidgetFilter<UpgradeType>> {
        return {
            name: 'FILTER.UPGRADE.TYPE',
            data: await this.findAll().pipe(take(1)).toPromise(),
            filterAction: async (input: {typeId: number}, selected) => input.typeId && input.typeId === selected.id
        };
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
