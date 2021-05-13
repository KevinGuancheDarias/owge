import { Injectable } from '@angular/core';
import { CriticalAttack, validContext } from '@owge/core';
import { AbstractCrudService, CrudServiceAuthControl, UniverseGameService } from '@owge/universe';

@Injectable()
export class AdminCriticalAttackService extends AbstractCrudService<CriticalAttack>{

    public constructor(universeGameService: UniverseGameService) {
        super(universeGameService);
    }

    protected _getEntity(): string {
        return 'critical-attack';
    }

    protected _getContextPathPrefix(): validContext {
        return 'admin';
    }

    protected _getAuthConfiguration(): CrudServiceAuthControl {
        return {
            findAll: false,
            findById: false
        };
    }
}
