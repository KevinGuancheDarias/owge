import { Injectable } from '@angular/core';
import { Observable, BehaviorSubject } from 'rxjs';
import { filter } from 'rxjs/operators';

import { UniverseGameService, UpgradeTypeStore, UpgradeType, WsEventCacheService, UniverseCacheManagerService } from '@owge/universe';

import { AbstractWebsocketApplicationHandler, StorageOfflineHelper } from '@owge/core';


@Injectable()
export class UpgradeTypeService extends AbstractWebsocketApplicationHandler {

  private _upgradeTypeStore: UpgradeTypeStore = new UpgradeTypeStore;
  private _offlineStore: StorageOfflineHelper<UpgradeType[]>;

  constructor(
    private _universeGameService: UniverseGameService,
    private _wsEventCacheService: WsEventCacheService,
    universeCacheManagerService: UniverseCacheManagerService
  ) {
    super();
    this._eventsMap = {
      upgrade_types_change: '_onChange'
    };
    this._offlineStore = universeCacheManagerService.getStore('upgrade_types.available');
  }

  public async workaroundSync(): Promise<void> {
    this._onChange(await this._wsEventCacheService.findFromCacheOrRun('upgrade_types_change', this._offlineStore, async () =>
      await this._universeGameService.getWithAuthorizationToUniverse('upgradeType/').toPromise()
    ));
  }

  public async workaroundInitialOffline(): Promise<void> {
    this._offlineStore.doIfNotNull(content => this._onChange(content));
  }

  public getUpgradeTypes(): Observable<UpgradeType[]> {
    return this._upgradeTypeStore.available.asObservable();
  }

  protected _onChange(content: UpgradeType[]) {
    this._upgradeTypeStore.available.next(content);
  }
}
