import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { validContext } from '@owge/core';
import {
    AbstractCrudService, CrudConfig, CrudServiceAuthControl, InterceptableSpeedGroup, Unit,
    UniverseGameService, WithImprovementsCrudMixin, WithRequirementsCrudMixin
} from '@owge/universe';
import { DisplayService } from '@owge/widgets';
import { Observable } from 'rxjs';
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

    public constructor(
        protected _universeGameService: UniverseGameService,
        protected _translateService: TranslateService,
        protected _displayService: DisplayService
    ) {
        super(_universeGameService);
        this._crudConfig = this.getCrudConfig();
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.10.0
     */
    public findInterceptedGroups(unitId: number): Observable<InterceptableSpeedGroup[]> {
        return this._universeGameService.requestWithAutorizationToContext('admin', 'get', `unit/${unitId}/interceptableSpeedGroups`);
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.10.0
     */
    public saveInterceptableGroups(unitId: number, interceptables: Partial<InterceptableSpeedGroup>[]): Observable<void> {
        return this._universeGameService.requestWithAutorizationToContext(
            'admin',
            'put',
            `unit/${unitId}/interceptableSpeedGroups`,
            interceptables
        );
    }

    public unsetCriticalAttack(unit: Unit) {
        return this._universeGameService.requestWithAutorizationToContext(
            this._getContextPathPrefix(),
            'delete',
            `unit/${unit.id}/criticalAttack`
        );
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
(AdminUnitService as any) = Mixin(WithImprovementsCrudMixin, WithRequirementsCrudMixin, AdminUnitService as any);
