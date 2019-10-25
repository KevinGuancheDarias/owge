import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { mix } from 'ts-mixer';

import { validContext, SessionStore, LoggerHelper } from '@owge/core';
import {
    WithRequirementsCrudMixin,
    UniverseGameService,
    AbstractCrudService,
    CrudServiceAuthControl,
    TimeSpecial,
    WithImprovementsCrudMixin,
    CrudConfig
} from '@owge/universe';
import { DisplayService } from '@owge/widgets';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable()
@mix(WithRequirementsCrudMixin, WithImprovementsCrudMixin)
export class AdminTimeSpecialService extends AbstractCrudService<TimeSpecial, number> {
    protected _log: LoggerHelper = new LoggerHelper(this.constructor.name);
    protected _crudConfig: CrudConfig;

    public constructor(
        protected _universeGameService: UniverseGameService,
        protected _sessionStore: SessionStore,
        protected _displayService: DisplayService,
        protected _translateService: TranslateService

    ) {
        super(_universeGameService);
        this._crudConfig = this.getCrudConfig();
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
export interface AdminTimeSpecialService
    extends AbstractCrudService<TimeSpecial, number>, WithRequirementsCrudMixin<number>, WithImprovementsCrudMixin<number> { }
