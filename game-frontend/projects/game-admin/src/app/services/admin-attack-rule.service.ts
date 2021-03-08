import { Injectable } from '@angular/core';
import { AbstractCrudService, CrudServiceAuthControl, UniverseGameService } from '@owge/universe';
import { validContext, AttackRule } from '@owge/core';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Injectable()
export class AdminAttackRuleService extends AbstractCrudService<AttackRule> {

    public constructor(universeGameService: UniverseGameService) {
        super(universeGameService);
    }

    protected _getEntity(): string {
        return 'attack-rule';
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
