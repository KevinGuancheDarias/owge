import { Injectable } from '@angular/core';
import { UniverseGameService } from './universe-game.service';
import { UniverseCacheManagerService } from './universe-cache-manager.service';
import { WsEventCacheService } from './ws-event-cache.service';
import { StorageOfflineHelper, AbstractWebsocketApplicationHandler } from '@owge/core';
import { PlanetListItem } from '../types/planet-list-item.type';
import { PlanetListStore } from '../storages/planet-list.store';
import { Observable } from 'rxjs';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Injectable()
export class PlanetListService extends AbstractWebsocketApplicationHandler {
    private _store: PlanetListStore = new PlanetListStore;
    private _offlineStore: StorageOfflineHelper<PlanetListItem[]>;

    public constructor(
        private _universeGameService: UniverseGameService,
        private _wsEventCacheService: WsEventCacheService,
        universeCacheManagerService: UniverseCacheManagerService
    ) {
        super();
        this._eventsMap = {
            planet_user_list_change: '_onPlanetUserListChange'
        };
        this._offlineStore = universeCacheManagerService.getStore('planet_list.list');
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public findAll(): Observable<PlanetListItem[]> {
        return this._store.list.asObservable();
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param planetId
     * @param  name
     */
    public add(planetId: number, name: string): Observable<void> {
        return this._universeGameService.requestWithAutorizationToContext('game', 'post', 'planet-list', { planetId, name });
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param {number} planetId
     * @returns {Observable<void>}
     */
    public delete(planetId: number): Observable<void> {
        return this._universeGameService.requestWithAutorizationToContext('game', 'delete', `planet-list/${planetId}`);
    }

    public async workaroundSync(): Promise<void> {
        this._onPlanetUserListChange(await this._wsEventCacheService.findFromCacheOrRun(
            'planet_user_list_change',
            this._offlineStore,
            async () => await this._universeGameService.requestWithAutorizationToContext('game', 'get', 'planet-list').toPromise()
        ));
    }

    public async workaroundInitialOffline(): Promise<void> {
        this._offlineStore.doIfNotNull(content => this._onPlanetUserListChange(content));
    }

    protected _onPlanetUserListChange(content: PlanetListItem[]): void {
        this._offlineStore.save(content);
        this._store.list.next(content);
    }
}
