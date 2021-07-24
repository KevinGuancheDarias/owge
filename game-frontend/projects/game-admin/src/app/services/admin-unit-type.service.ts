import { Injectable } from '@angular/core';
import { UnitType, validContext } from '@owge/core';
import { AbstractCrudService, CrudServiceAuthControl, UniverseGameService } from '@owge/universe';
import { WidgetFilter } from '@owge/core';
import { Observable } from 'rxjs';
import { take } from 'rxjs/operators';


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

    public async buildFilter(): Promise<WidgetFilter<UnitType>> {
        return {
            name: 'FILTER.UNIT.TYPE',
            data: await this.findAll().pipe(take(1)).toPromise(),
            filterAction: async (input: {typeId: number}, selected) => input.typeId && input.typeId === selected.id
        };
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param unitType
     * @returns
     */
    public unsetAttackRule(unitType: UnitType): Observable<void> {
        return this._universeGameService.requestWithAutorizationToContext(
            this._getContextPathPrefix(),
            'delete',
            `unit_type/${unitType.id}/attackRule`
        );
    }

    public unsetCriticalAttack(unitType: UnitType) {
        return this._universeGameService.requestWithAutorizationToContext(
            this._getContextPathPrefix(),
            'delete',
            `unit_type/${unitType.id}/criticalAttack`
        );
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
