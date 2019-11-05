import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { WithReadCrudMixin, TimeSpecial, CrudServiceAuthControl, UniverseGameService, ActiveTimeSpecialType } from '@owge/universe';
import { validContext } from '@owge/core/owge-core';

/**
 * Service to handle time special operations
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 * @class TimeSpecialService
 * @extends {WithReadCrudMixin<TimeSpecial, number>}
 */
@Injectable()
export class TimeSpecialService extends WithReadCrudMixin<TimeSpecial, number> {


    public constructor(protected _universeGameService: UniverseGameService) {
        super();
    }

    public activate(timeSpecialId: number): Observable<ActiveTimeSpecialType> {
        return this._universeGameService.requestWithAutorizationToContext('game', 'post', `${this._getEntity()}/activate`, timeSpecialId);
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
        return 'game';
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
