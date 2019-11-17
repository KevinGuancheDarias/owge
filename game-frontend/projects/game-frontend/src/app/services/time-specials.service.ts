import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { validContext } from '@owge/core';
import { WithReadCrudMixin, TimeSpecial, CrudServiceAuthControl, UniverseGameService, ActiveTimeSpecialType } from '@owge/universe';

import { map } from 'rxjs/operators';

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


    /**
     * Activates a time special
     *
     * @todo When Feature/124 is done, the need to use reload improvements disappear,
     *  and TimeSpecials should register a listener for time_special status change
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param {number} timeSpecialId
     * @returns {Observable<ActiveTimeSpecialType>}
     */
    public activate(timeSpecialId: number): Observable<ActiveTimeSpecialType> {
        return this._universeGameService.requestWithAutorizationToContext(
            'game',
            'post',
            `${this._getEntity()}/activate`, timeSpecialId
        ).pipe(map(activatedTimeSpecial => {
            this._universeGameService.reloadImprovement().then(improvement =>
                this._log.debug('As you have activated a time special the improvements has been reloaded', <any>improvement)
            );
            return activatedTimeSpecial;
        }));
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
