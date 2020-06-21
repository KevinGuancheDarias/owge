import { Injectable } from '@angular/core';

import {
    AbstractCrudService,
    UniverseGameService,
    CrudServiceAuthControl,
    WithRequirementsCrudMixin,
    WithImprovementsCrudMixin,
    CrudConfig,
    RequirementInformation
} from '@owge/universe';
import { Faction } from '@owge/faction';
import { validContext, ProgrammingError } from '@owge/core';
import { Mixin } from 'ts-mixer';
import { take } from 'rxjs/operators';
import { WidgetFilter } from '@owge/widgets';


export interface AdminFactionService
    extends AbstractCrudService<Faction>, WithRequirementsCrudMixin<number>, WithImprovementsCrudMixin<number> { }

/**
 * Has methods related with the CRUD of the Faction
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 * @class AdminFactionService
 * @extends {AbstractCrudService<Faction>}
 */
@Injectable()
export class AdminFactionService extends AbstractCrudService<Faction> {

    protected _crudConfig: CrudConfig;

    public constructor(protected _universeGameService: UniverseGameService) {
        super(_universeGameService);
        this._crudConfig = this.getCrudConfig();
    }


    /**
     * Will filter the input by the been faction requirement
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public async buildFilter(): Promise<WidgetFilter<Faction>> {
        return {
            name: 'FILTER.BY_FACTION',
            data: await this.findAll().pipe(take(1)).toPromise(),
            filterAction: async (input, selectedFaction) => {
                const requirements: RequirementInformation[] = input.requirements;
                if (!requirements) {
                    throw new ProgrammingError('Can NOT filter when the input has not requirements');
                }
                return requirements.some(requirement =>
                    requirement.requirement.code === 'BEEN_RACE' && requirement.secondValue === selectedFaction.id
                );
            }
        };
    }

    protected _getEntity(): string {
        return 'faction';
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
(<any>AdminFactionService) = Mixin(WithImprovementsCrudMixin, WithRequirementsCrudMixin, <any>AdminFactionService);
