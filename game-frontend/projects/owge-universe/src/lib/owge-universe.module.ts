import { NgModule, ModuleWithProviders, Injector } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { CoreModule } from '@owge/core';

import { UniverseStorage } from './storages/universe.storage';
import { UniverseInitializer } from './initializers/universe.initializer';
import { UniverseService } from './services/universe.service';
import { UniverseGameService } from './services/universe-game.service';
import { ImageSelectorComponent } from './components/image-selector/image-selector.component';
import { OwgeWidgetsModule } from '@owge/widgets';
import { ImageStoreService } from './services/image-store.service';
import { MissionStore } from './storages/mission.store';
import { ResourceManagerService } from './services/resource-manager.service';
import { UniverseCacheManagerService } from './services/universe-cache-manager.service';
import { OnClickIfWsConnectedDirective } from './directives/on-click-if-ws-conected.directive';
import { WebsocketService } from './services/websocket.service';
import { WsEventCacheService } from './services/ws-event-cache.service';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
@NgModule({
  imports: [
    CommonModule,
    CoreModule,
    OwgeWidgetsModule,
    TranslateModule.forChild()
  ],
  declarations: [ImageSelectorComponent, OnClickIfWsConnectedDirective],
  providers: [
    ImageStoreService
  ],
  exports: [
    ImageSelectorComponent,
    OnClickIfWsConnectedDirective
  ]
})
export class OwgeUniverseModule {
  public static forRoot(): ModuleWithProviders<OwgeUniverseModule> {
    return {
      ngModule: OwgeUniverseModule,
      providers: [
        UniverseStorage,
        UniverseGameService,
        UniverseService,
        UniverseInitializer,
        OwgeWidgetsModule,
        ImageStoreService,
        MissionStore,
        ResourceManagerService,
        UniverseCacheManagerService,
        WebsocketService,
        WsEventCacheService
      ]
    };
  }

  public constructor(_injector: Injector, private _websocketService: WebsocketService) {
    _websocketService.addEventHandler(
      _injector.get(UniverseGameService)
    );
  }
}
