import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { validContext } from '@owge/core';
import {
    AbstractCrudService, CrudConfig, CrudServiceAuthControl, UniverseGameService, Upgrade,
    WithImprovementsCrudMixin, WithRequirementsCrudMixin
} from '@owge/universe';
import { DisplayService } from '@owge/widgets';
import { Mixin } from 'ts-mixer';


export interface AdminUpgradeService
    extends AbstractCrudService<Upgrade, number>, WithRequirementsCrudMixin<number>, WithImprovementsCrudMixin<number> { }

/**
 * Has methods related with the CRUD of the Upgrade
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable()
export class AdminUpgradeService extends AbstractCrudService<Upgrade, number> {

    protected _crudConfig: CrudConfig;

    public constructor(
        protected _universeGameService: UniverseGameService,
        protected _translateService: TranslateService,
        protected _displayService: DisplayService
    ) {
        super(_universeGameService);
        this._crudConfig = this.getCrudConfig();
    }

    protected _getEntity(): string {
        return 'upgrade';
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
(AdminUpgradeService as any) = Mixin(WithImprovementsCrudMixin, WithRequirementsCrudMixin, AdminUpgradeService as any);
