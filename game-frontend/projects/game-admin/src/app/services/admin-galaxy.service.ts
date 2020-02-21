import { Injectable } from '@angular/core';
import { AbstractCrudService, UniverseGameService, CrudServiceAuthControl } from '@owge/universe';
import { validContext } from '@owge/core';
import { Galaxy } from '@owge/galaxy';
import { Observable } from 'rxjs';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Injectable()
export class AdminGalaxyService extends AbstractCrudService<Galaxy> {
    public constructor(protected _universeGameService: UniverseGameService) {
        super(_universeGameService);
    }

    /**
     * Returns true if the specified galaxy has players
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param id The galaxy id
     * @returns
     */
    public hasPlayers(id: number): Observable<boolean> {
        return this._universeGameService.requestWithAutorizationToContext('admin', 'get', `galaxy/${id}/has-players`);
    }

    protected _getEntity(): string {
        return 'galaxy';
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
