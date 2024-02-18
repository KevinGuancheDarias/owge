import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { UniverseGameService, UpgradeTypeStore, WsEventCacheService, UniverseCacheManagerService } from '@owge/universe';
import { UpgradeType } from '@owge/types/universe';

import { AbstractWebsocketApplicationHandler, StorageOfflineHelper } from '@owge/core';


@Injectable()
export class UpgradeTypeService extends AbstractWebsocketApplicationHandler {

  private _upgradeTypeStore: UpgradeTypeStore = new UpgradeTypeStore;
  private _offlineStore: StorageOfflineHelper<UpgradeType[]>;

  constructor(
    private _universeGameService: UniverseGameService,
    private _wsEventCacheService: WsEventCacheService,
    private _universeCacheManagerService: UniverseCacheManagerService
  ) {
    super();
    this._eventsMap = {
      upgrade_types_change: '_onChange'
    };
  }

  public async createStores(): Promise<void> {
    this._offlineStore = this._universeCacheManagerService.getStore('upgrade_types.available');
  }

  public async workaroundInitialOffline(): Promise<void> {
    await this._offlineStore.doIfNotNull(content => this._onChange(content));
  }

  public getUpgradeTypes(): Observable<UpgradeType[]> {
    return this._upgradeTypeStore.available.asObservable();
  }

  protected _onChange(content: UpgradeType[]) {
    this._upgradeTypeStore.available.next(content);
  }
}
