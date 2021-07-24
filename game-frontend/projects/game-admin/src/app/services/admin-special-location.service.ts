import { Injectable } from '@angular/core';
import { validContext, WidgetFilter } from '@owge/core';
import {
    AbstractCrudService, CrudConfig, CrudServiceAuthControl, SpecialLocation,
    UniverseGameService, WithImprovementsCrudMixin, WidgetFilterUtil
} from '@owge/universe';
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

    public buildRequiresSpecialLocationFilter(): WidgetFilter<any> {
        return {
            name: 'FILTER.SPECIAL_LOCATION.BY_REQUIRES',
            inputType: 'checkbox',
            filterAction: async (input) => WidgetFilterUtil.runRequirementsFilter(
                    input,
                    requirement => requirement.requirement.code === 'HAVE_SPECIAL_LOCATION'
                )
        };
    }

    /**
     * Will filter the input by the been faction requirement
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
     public async buildFilterByRequires(): Promise<WidgetFilter<SpecialLocation>> {
        return {
            name: 'FILTER.SPECIAL_LOCATION.REQUIRES_SPECIFIED',
            data: await this.findAll().pipe(take(1)).toPromise(),
            filterAction: async (input, selectedSpecialLocation) =>
                WidgetFilterUtil.runRequirementsFilter(
                    input,
                    requirement =>
                        requirement.requirement.code === 'HAVE_SPECIAL_LOCATION' && requirement.secondValue === selectedSpecialLocation.id
                )
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
(AdminSpecialLocationService as any) = Mixin(WithImprovementsCrudMixin, AdminSpecialLocationService as any);

