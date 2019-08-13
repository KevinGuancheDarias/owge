import { UserService } from './app/service/user.service';
import { FactionService } from './app/faction/faction.service';
import { UniverseService } from './app/universe/universe.service';
import { APP_BASE_HREF } from '@angular/common';
import { Angular2FontawesomeModule } from 'angular2-fontawesome/angular2-fontawesome';
import { PlanetService } from './app/service/planet.service';
import { UnitService } from './app/service/unit.service';
import { UpgradeService } from './app/service/upgrade.service';
import { ResourceManagerService } from './app/service/resource-manager.service';
import { LoginSessionService } from './app/login-session/login-session.service';
import { LoginService } from './app/login/login.service';
import { APP_ROUTES } from './app/app.module';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { DeployedUnitsBigComponent } from './app/deployed-units-big/deployed-units-big.component';
import { DisplaySingleUnitComponent } from './app/display-single-unit/display-single-unit.component';
import { BuildUnitsComponent } from './app/build-units/build-units.component';
import { DisplaySingleUpgradeComponent } from './app/display-single-upgrade/display-single-upgrade.component';
import { UnitsComponent } from './app/units/units.component';
import { DisplayRequirementsComponent } from './app/display-requirements/display-requirements.component';
import { UpgradesComponent } from './app/upgrades/upgrades.component';
import { DisplayDynamicImageComponent } from './app/display-dynamic-image/display-dynamic-image.component';
import { DisplaySingleResourceComponent } from './app/display-single-resource/display-single-resource.component';
import { DisplaySinglePlanetComponent } from './app/display-single-planet/display-single-planet.component';
import { SideBarComponent } from './app/side-bar/side-bar.component';
import { DisplaySingleFactionComponent } from './app/display-single-faction/display-single-faction.component';
import { FactionSelectorComponent } from './app/faction-selector/faction-selector.component';
import { BaseComponent } from './app/base/base.component';
import { GameIndexComponent } from './app/game-index/game-index.component';
import { DisplaySingleUniverseComponent } from './app/display-single-universe/display-single-universe.component';
import { UniverseSelectionComponent } from './app/universe-selection/universe-selection.component';
import { PageNotFoundComponent } from './app/page-not-found/page-not-found.component';
import { LoginComponent } from './app/login/login.component';
import { AppComponent } from './app/app.component';

import { TestModuleMetadata } from '@angular/core/testing';

export let testingConfig: TestModuleMetadata = {
  declarations: [
    AppComponent,
    LoginComponent,
    PageNotFoundComponent,
    UniverseSelectionComponent,
    DisplaySingleUniverseComponent,
    GameIndexComponent,
    SideBarComponent,
    BaseComponent,
    FactionSelectorComponent,
    DisplaySingleFactionComponent,
    DisplaySinglePlanetComponent,
    DisplaySingleResourceComponent,
    DisplayDynamicImageComponent,
    UpgradesComponent,
    DisplaySingleUpgradeComponent,
    DisplayRequirementsComponent,
    UnitsComponent,
    BuildUnitsComponent,
    DeployedUnitsBigComponent,
    DisplaySingleUnitComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    RouterModule.forRoot(APP_ROUTES),
    Angular2FontawesomeModule
  ],
  providers: [
    [{ provide: APP_BASE_HREF, useValue: '/' }],
    LoginService,
    LoginSessionService,
    ResourceManagerService,
    UpgradeService,
    UnitService,
    PlanetService,
    UniverseService,
    FactionService,
    UserService
  ],
};
