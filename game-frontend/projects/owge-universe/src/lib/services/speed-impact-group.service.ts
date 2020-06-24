import { AbstractWebsocketApplicationHandler, StorageOfflineHelper } from '@owge/core';
import { UniverseCacheManagerService } from './universe-cache-manager.service';
import { WsEventCacheService } from './ws-event-cache.service';
import { UniverseGameService } from './universe-game.service';
import { SpeedImpactGroupStore } from '../storages/speed-impact-group.store';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Injectable()
export class SpeedImpactGroupService extends AbstractWebsocketApplicationHandler {
    private _offlineUnlockedIdsStore: StorageOfflineHelper<number[]>;
    private _store: SpeedImpactGroupStore = new SpeedImpactGroupStore;

    public constructor(
        private _universeGameService: UniverseGameService,
        private _wsEventCacheService: WsEventCacheService,
        private _universeCacheManagerService: UniverseCacheManagerService
    ) {
        super();
        this._eventsMap = {
            speed_impact_group_unlocked_change: '_onUnlockedChange'
        };
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public findunlockedIds(): Observable<number[]> {
        return this._store.unlockedIds.asObservable();
    }

    public async createStores(): Promise<void> {
        this._offlineUnlockedIdsStore = this._universeCacheManagerService.getStore('speed_impact_group.unlocked_ids');
    }

    public async workaroundSync(): Promise<void> {
        this._onUnlockedChange(await this._wsEventCacheService.findFromCacheOrRun(
            'speed_impact_group_unlocked_change',
            this._offlineUnlockedIdsStore,
            async () => await this._universeGameService.requestWithAutorizationToContext('game', 'get', 'speed-impact-group/unlocked-ids')
                .toPromise()
        ));
    }

    public async workaroundInitialOffline(): Promise<void> {
        await this._offlineUnlockedIdsStore.doIfNotNull(content => this._onUnlockedChange(content));
    }

    protected async _onUnlockedChange(content: number[]): Promise<void> {
        await this._offlineUnlockedIdsStore.save(content);
        this._store.unlockedIds.next(content);
    }
}
