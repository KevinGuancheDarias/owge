import { NgModule, ModuleWithProviders, Injector } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { CoreModule } from '@owge/core';

import { UniverseStorage } from './storages/universe.storage';
import { UniverseInitializer } from './initializers/universe.initializer';
import { UniverseService } from './services/universe.service';
import { ClockInitializer } from './initializers/clock.initializer';
import { ClockSyncService } from './services/clock-sync.service';
import { UniverseGameService } from './services/universe-game.service';
import { ImageSelectorComponent } from './components/image-selector/image-selector.component';
import { OwgeWidgetsModule } from '@owge/widgets';
import { ImageStoreService } from './services/image-store.service';
import { MissionStore } from './storages/mission.store';
import { ResourceManagerService } from './services/resource-manager.service';
import { WebsocketService } from 'projects/game-frontend/src/app/service/websocket.service';

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
  declarations: [ImageSelectorComponent],
  providers: [
    ImageStoreService
  ],
  exports: [
    ImageSelectorComponent
  ]
})
export class OwgeUniverseModule {
  public static forRoot(): ModuleWithProviders {
    return {
      ngModule: OwgeUniverseModule,
      providers: [
        UniverseStorage,
        UniverseGameService,
        UniverseService,
        ClockSyncService,
        UniverseInitializer,
        ClockInitializer,
        OwgeWidgetsModule,
        ImageStoreService,
        MissionStore,
        ResourceManagerService
      ]
    };
  }

  public constructor(_injector: Injector, private _websocketService: WebsocketService) {
    _websocketService.addEventHandler(
      _injector.get(UniverseGameService)
    );
  }
}
