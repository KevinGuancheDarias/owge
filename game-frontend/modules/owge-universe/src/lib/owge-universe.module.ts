import { NgModule, ModuleWithProviders, Injector } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { ToastrModule } from 'ngx-toastr';

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
import { PlanetDescriptionPipe } from './pipes/planet-description.pipe';
import { FormsModule } from '@angular/forms';
import { PlanetOwnerPipe } from './pipes/planet-owner.pipe';
import { SpeedImpactGroupService } from './services/speed-impact-group.service';
import { TutorialService } from './services/tutorial.service';
import { UnitTypeService } from './services/unit-type.service';
import { UnitRuleFinderService } from './services/unit-rule-finder.service';
import { RuleService } from './services/rule.service';
import { MissionService } from './services/mission.service';
import {TimeSpecialService} from './services/time-specials.service';
import {ActiveTimeSpecialService} from './services/active-time-special.service';
import {ActiveTimeSpecialRuleFinderService} from './services/active-time-special-rule-finder.service';

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
    FormsModule,
    CoreModule,
    OwgeWidgetsModule,
    TranslateModule.forChild(),
    ToastrModule.forRoot()
  ],
  declarations: [
    ImageSelectorComponent, OnClickIfWsConnectedDirective, PlanetDescriptionPipe,
    PlanetOwnerPipe
  ],
  providers: [
    ImageStoreService
  ],
  exports: [
    ImageSelectorComponent,
    OnClickIfWsConnectedDirective,
    PlanetDescriptionPipe,
    PlanetOwnerPipe
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
        WsEventCacheService,
        SpeedImpactGroupService,
        TutorialService,
        UnitTypeService,
        RuleService,
        UnitRuleFinderService,
        MissionService,
        TimeSpecialService,
        ActiveTimeSpecialService,
        ActiveTimeSpecialRuleFinderService
      ]
    };
  }

  constructor(_injector: Injector, private _websocketService: WebsocketService) {
    _websocketService.addEventHandler(
      _injector.get(UniverseGameService),
      _injector.get(UniverseCacheManagerService),
      _injector.get(SpeedImpactGroupService),
      _injector.get(TutorialService),
      _injector.get(UnitTypeService),
      _injector.get(RuleService),
      _injector.get(MissionService)
    );
  }
}
