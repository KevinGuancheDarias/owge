import {AbstractWebsocketApplicationHandler, CommonEntity, StorageOfflineHelper} from '@owge/core';
import { UniverseCacheManagerService } from './universe-cache-manager.service';
import { WsEventCacheService } from './ws-event-cache.service';
import { UniverseGameService } from './universe-game.service';
import { SpeedImpactGroupStore } from '../storages/speed-impact-group.store';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {map} from 'rxjs/operators';

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
    private offlineContent: StorageOfflineHelper<CommonEntity[]>;
    private _store: SpeedImpactGroupStore = new SpeedImpactGroupStore;

    constructor(
        private _universeGameService: UniverseGameService,
        private _wsEventCacheService: WsEventCacheService,
        private _universeCacheManagerService: UniverseCacheManagerService
    ) {
        super();
        this._eventsMap = {
            speed_impact_group_unlocked_change: '_onUnlockedChange',
            speed_group_change: 'onSpeedGroupChange'
        };
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    findunlockedIds(): Observable<number[]> {
        return this._store.unlockedIds.asObservable();
    }

    findById(id: number): Observable<CommonEntity> {
        return this._store.content.asObservable()
            .pipe(
                map(speedGroups => speedGroups.find(current => current.id === id))
            );
    }

    async createStores(): Promise<void> {
        this._offlineUnlockedIdsStore = this._universeCacheManagerService.getStore('speed_impact_group.unlocked_ids');
        this.offlineContent = this._universeCacheManagerService.getStore('speed_impact_group.content');
    }

    async workaroundInitialOffline(): Promise<void> {
        await this._offlineUnlockedIdsStore.doIfNotNull(content => this._onUnlockedChange(content));
    }

    protected async _onUnlockedChange(content: number[]): Promise<void> {
        await this._offlineUnlockedIdsStore.save(content);
        this._store.unlockedIds.next(content);
    }

    protected async onSpeedGroupChange(content: CommonEntity[]): Promise<void> {
        await this.offlineContent.save(content);
        this._store.content.next(content);
    }
}
