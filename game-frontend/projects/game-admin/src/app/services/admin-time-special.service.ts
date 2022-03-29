import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { LoggerHelper, ProgrammingError, RequirementInformation, SessionStore, validContext } from '@owge/core';
import {
    AbstractCrudService, CrudConfig, CrudServiceAuthControl,
    TimeSpecial, UniverseGameService, WithImprovementsCrudMixin, WithRequirementsCrudMixin
} from '@owge/universe';
import { DisplayService, WidgetFilter, WidgetFilterUtil } from '@owge/widgets';
import { take } from 'rxjs/operators';
import { Mixin } from 'ts-mixer';
import { AdminFactionService } from './admin-faction.service';


export interface AdminTimeSpecialService
    extends AbstractCrudService<TimeSpecial, number>, WithRequirementsCrudMixin<TimeSpecial, number>, WithImprovementsCrudMixin<number> { }
/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable()
export class AdminTimeSpecialService extends AbstractCrudService<TimeSpecial, number> {
    protected _log: LoggerHelper = new LoggerHelper(this.constructor.name);
    protected _crudConfig: CrudConfig;

    public constructor(
        protected _universeGameService: UniverseGameService,
        protected _sessionStore: SessionStore,
        protected _displayService: DisplayService,
        protected _translateService: TranslateService,
        protected adminFactionService: AdminFactionService

    ) {
        super(_universeGameService);
        this._crudConfig = this.getCrudConfig();
    }

    public async buildFilter(): Promise<WidgetFilter<TimeSpecial>[]> {
        return [{
            name: 'FILTER.BY_TIME_SPECIAL_AVAILABLE',
            data: await this.findAll().pipe(take(1)).toPromise(),
            filterAction: async (input, selectedSpecialLocation) => {
                const requirements: RequirementInformation[] = input.requirements;
                if (!requirements) {
                    throw new ProgrammingError('Can NOT filter when the input has not requirements');
                }
                return requirements.some(requirement =>
                    requirement.requirement.code === 'HAVE_SPECIAL_AVAILABLE' && requirement.secondValue === selectedSpecialLocation.id
                );
            },
            dataSelectionFilter: [
                await this.adminFactionService.buildFilter(), WidgetFilterUtil.buildByNameFilter()
            ]
        }, {
            name: 'FILTER.BY_TIME_SPECIAL_ENABLED',
            data: await this.findAll().pipe(take(1)).toPromise(),
            filterAction: async (input, selectedSpecialLocation) => {
                const requirements: RequirementInformation[] = input.requirements;
                if (!requirements) {
                    throw new ProgrammingError('Can NOT filter when the input has not requirements');
                }
                return requirements.some(requirement =>
                    requirement.requirement.code === 'HAVE_SPECIAL_ENABLED' && requirement.secondValue === selectedSpecialLocation.id
                );
            },
            dataSelectionFilter: [
                await this.adminFactionService.buildFilter()
            ]
        }];
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @protected
     * @returns
     */
    protected _getEntity(): string {
        return 'time_special';
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @protected
     * @returns
     */
    protected _getContextPathPrefix(): validContext {
        return 'admin';
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @protected
     * @returns
     */
    protected _getAuthConfiguration(): CrudServiceAuthControl {
        return {
            findAll: true,
            findById: true
        };
    }


}
(<any>AdminTimeSpecialService) = Mixin(WithImprovementsCrudMixin, WithRequirementsCrudMixin, <any>AdminTimeSpecialService);
