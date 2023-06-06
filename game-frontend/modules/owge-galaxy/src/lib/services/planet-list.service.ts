import { Injectable } from '@angular/core';
import { StorageOfflineHelper, AbstractWebsocketApplicationHandler } from '@owge/core';
import { Observable, Subscription } from 'rxjs';
import { UniverseGameService, WsEventCacheService, UniverseCacheManagerService } from '@owge/universe';
import { PlanetListStore } from '../stores/planet-list.store';
import { PlanetListItem } from '../types/planet-list-item.type';
import { PlanetService } from './planet.service';

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
    private _list: PlanetListItem[] = [];
    private _planetExploredSubscription: Subscription;

    public constructor(
        private _universeGameService: UniverseGameService,
        private _wsEventCacheService: WsEventCacheService,
        private _universeCacheManagerService: UniverseCacheManagerService,
        private _planetService: PlanetService
    ) {
        super();
        this._eventsMap = {
            planet_user_list_change: '_onPlanetUserListChange'
        };
        this._store.list.subscribe(list => this._list = list);
    }

    public async createStores(): Promise<void> {
        this._offlineStore = this._universeCacheManagerService.getStore('planet_list.list');
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
    public async workaroundInitialOffline(): Promise<void> {
        await this._offlineStore.doIfNotNull(content => this._onPlanetUserListChange(content));
    }

    protected async _onPlanetUserListChange(content: PlanetListItem[]): Promise<void> {
        if (this._planetExploredSubscription) {
            this._planetExploredSubscription.unsubscribe();
            delete this._planetExploredSubscription;
        }
        this._planetExploredSubscription = this._planetService.onPlanetExplored().subscribe(async planet => {
            if (planet) {
                const index = this._list.findIndex(current => current.planet.id === planet.id);
                if (index !== -1) {
                    this._list[index].planet = planet;
                }
            } else {
                this._list = await this._getFromBackend();
            }
            await this._offlineStore.save(this._list);
            this._store.list.next(this._list);
        });
        await this._offlineStore.save(content);
        this._store.list.next(content);

    }

    private _getFromBackend(): Promise<PlanetListItem[]> {
        return this._universeGameService.requestWithAutorizationToContext('game', 'get', 'planet-list').toPromise();
    }
}
