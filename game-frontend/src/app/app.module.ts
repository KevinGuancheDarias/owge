import { SidebarModule } from 'ng-sidebar/lib';
import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { RouterModule, Routes } from '@angular/router';
import { Angular2FontawesomeModule } from 'angular2-fontawesome/angular2-fontawesome';
import { Injector } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { ServiceLocator } from './service-locator/service-locator';
import { LoginService } from './login/login.service';
import { LoginSessionService } from './login-session/login-session.service';
import { NavigationService } from './service/navigation.service';
import { PlanetService } from './service/planet.service';
import { UnitService } from './service/unit.service';
import { KgdNg2WidgetsModule } from 'kgd-ng2-widgets';
import { UpgradeService } from './service/upgrade.service';
import { ResourceManagerService } from './service/resource-manager.service';

import { AppComponent } from './app.component';
import { LoginComponent } from './login/login.component';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';
import { UniverseSelectionComponent } from './universe-selection/universe-selection.component';
import { DisplaySingleUniverseComponent } from './display-single-universe/display-single-universe.component';
import { GameIndexComponent } from './game-index/game-index.component';
import { SideBarComponent } from './side-bar/side-bar.component';
import { BaseComponent } from './base/base.component';
import { FactionSelectorComponent } from './faction-selector/faction-selector.component';
import { DisplaySingleFactionComponent } from './display-single-faction/display-single-faction.component';
import { DisplaySinglePlanetComponent } from './display-single-planet/display-single-planet.component';
import { DisplaySingleResourceComponent } from './display-single-resource/display-single-resource.component';
import { DisplayDynamicImageComponent } from './display-dynamic-image/display-dynamic-image.component';
import { UpgradesComponent } from './upgrades/upgrades.component';
import { DisplaySingleUpgradeComponent } from './display-single-upgrade/display-single-upgrade.component';
import { DisplayRequirementsComponent } from './display-requirements/display-requirements.component';
import { UnitsComponent } from './units/units.component';
import { BuildUnitsComponent } from './build-units/build-units.component';
import { DeployedUnitsBigComponent } from './deployed-units-big/deployed-units-big.component';
import { DisplaySingleUnitComponent } from './display-single-unit/display-single-unit.component';
import { LoadingComponent } from './loading/loading.component';
import { NavigationComponent } from './components/navigation/navigation.component';
import { NavigationControlsComponent } from './components/navigation-controls/navigation-controls.component';
import { DisplayQuadrantComponent } from './components/display-quadrant/display-quadrant.component';
import { PlanetDisplayNamePipe } from './pipes/planet-display-name/planet-display-name.pipe';
import { ModalComponent } from './components/modal/modal.component';
import { WebsocketService } from './service/websocket.service';
import { PingWebsocketApplicationHandler } from './class/ping-websocket-application-handler';
import { DeployedUnitsListComponent } from './components/deployed-units-list/deployed-units-list.component';
import { MissionService } from './services/mission.service';
import { LoadingService } from './services/loading.service';
import { ReportsListComponent } from './components/reports-list/reports-list.component';
import { ReportService } from './services/report.service';
import { UnitsAliveDeathListComponent } from './components/units-alive-death-list/units-alive-death-list.component';
import { ListRunningMissionsComponent } from './components/list-running-missions/list-running-missions.component';
import { DisplayUsernamePipe } from './pipes/display-username.pipe';
import { DisplayMissionTypePipe } from './pipes/display-mission-type.pipe';


export const APP_ROUTES: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'universe-selection', component: UniverseSelectionComponent, canActivate: [LoginSessionService] },
  { path: 'home', component: GameIndexComponent, canActivate: [LoginSessionService] },
  { path: 'upgrades', component: UpgradesComponent, canActivate: [LoginSessionService] },
  { path: 'units', component: UnitsComponent, canActivate: [LoginSessionService] },
  { path: 'units/build', component: UnitsComponent, canActivate: [LoginSessionService] },
  { path: 'units/deployed', component: UnitsComponent, canActivate: [LoginSessionService] },
  { path: 'navigate', component: NavigationComponent, canActivate: [LoginSessionService] },
  { path: 'reports', component: ReportsListComponent, canActivate: [LoginSessionService] },
  { path: '**', component: PageNotFoundComponent }
];

@NgModule({
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
    DisplaySingleUnitComponent,
    LoadingComponent,
    NavigationComponent,
    NavigationControlsComponent,
    DisplayQuadrantComponent,
    PlanetDisplayNamePipe,
    ModalComponent,
    DeployedUnitsListComponent,
    ReportsListComponent,
    UnitsAliveDeathListComponent,
    ListRunningMissionsComponent,
    DisplayUsernamePipe,
    DisplayMissionTypePipe
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    Angular2FontawesomeModule,
    RouterModule.forRoot(APP_ROUTES),
    SidebarModule,
    KgdNg2WidgetsModule,
    HttpClientModule,
    NgbModule.forRoot()
  ],
  providers: [
    LoginService,
    LoginSessionService,
    ResourceManagerService,
    UpgradeService,
    UnitService,
    PlanetService,
    NavigationService,
    WebsocketService,
    MissionService,
    LoadingService,
    ReportService
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  constructor(private injector: Injector, private _websocketService: WebsocketService) {
    ServiceLocator.injector = this.injector;
    window['globalShit'] = this._websocketService;
    this._websocketService.addEventHandler(new PingWebsocketApplicationHandler());
    this._websocketService.initSocket('http://127.0.0.1:3000');
  }
}
