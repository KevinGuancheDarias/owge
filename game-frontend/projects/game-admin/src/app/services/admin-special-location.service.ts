import { Injectable } from '@angular/core';
import { ProgrammingError, RequirementInformation, validContext } from '@owge/core';
import {
    AbstractCrudService, CrudConfig, CrudServiceAuthControl, SpecialLocation, UniverseGameService, WithImprovementsCrudMixin
} from '@owge/universe';
import { WidgetFilter } from '@owge/widgets';
import { take } from 'rxjs/operators';
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

    public async buildFilter(): Promise<WidgetFilter<SpecialLocation>> {
        return {
            name: 'FILTER.BY_SPECIAL_LOCATION',
            data: await this.findAll().pipe(take(1)).toPromise(),
            filterAction: async (input, selectedSpecialLocation) => {
                const requirements: RequirementInformation[] = input.requirements;
                if (!requirements) {
                    throw new ProgrammingError('Can NOT filter when the input has not requirements');
                }
                return requirements.some(requirement =>
                    requirement.requirement.code === 'HAVE_SPECIAL_LOCATION' && requirement.secondValue === selectedSpecialLocation.id
                );
            }
        };
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

