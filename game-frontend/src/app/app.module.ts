import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { RouterModule, Routes } from '@angular/router';
import { Angular2FontawesomeModule } from 'angular2-fontawesome/angular2-fontawesome';
import { Injector } from '@angular/core';
import { HttpClientModule, HttpClient } from '@angular/common/http';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { ServiceLocator } from './service-locator/service-locator';
import { LoginService } from './login/login.service';
import { LoginSessionService } from './login-session/login-session.service';
import { NavigationService } from './service/navigation.service';
import { PlanetService } from './service/planet.service';
import { UnitService } from './service/unit.service';
import { UpgradeService } from './service/upgrade.service';
import { ResourceManagerService } from './service/resource-manager.service';

import { AppComponent } from './app.component';
import { LoginComponent } from './login/login.component';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';
import { UniverseSelectionComponent } from './universe-selection/universe-selection.component';
import { DisplaySingleUniverseComponent } from './display-single-universe/display-single-universe.component';
import { GameIndexComponent } from './game-index/game-index.component';
import { SideBarComponent } from './side-bar/side-bar.component';
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
import { NavigationComponent } from './components/navigation/navigation.component';
import { NavigationControlsComponent } from './components/navigation-controls/navigation-controls.component';
import { DisplayQuadrantComponent } from './components/display-quadrant/display-quadrant.component';
import { PlanetDisplayNamePipe } from './pipes/planet-display-name/planet-display-name.pipe';
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
import { SynchronizeCredentialsComponent } from './components/synchronize-credentials/synchronize-credentials.component';
import { SanitizeService } from './services/sanitize.service';
import { VersionInformationComponent } from './components/version-information/version-information.component';
import { UnitRequirementsComponent } from './components/unit-requirements/unit-requirements.component';
import { UnitTypeService } from './services/unit-type.service';
import { UpgradeTypeService } from './services/upgrade-type.service';
import { CountdownComponent } from './components/countdown/countdown.component';
import { MilisToDatePipe } from './pipes/milis-to-date/milis-to-date.pipe';
import { PlanetSelectorComponent } from './components/planet-selector/planet-selector.component';
import { MissionModalComponent } from './mission-modal/mission-modal.component';
import { UserModule } from './modules/user/user.module';
import { ALLIANCE_ROUTES } from './modules/alliance/alliance.routes';
import { RouterRootComponent } from './modules/core/components/router-root/router-root.component';
import { AllianceModule } from './modules/alliance/alliance.module';
import { CoreModule } from './modules/core/core.module';
import { UniverseModule } from './modules/universe/universe.module';
import { WidgetsModule } from './modules/widgets/widgets.module';
import { TranslateModule, TranslateLoader, TranslateService } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { RANKING_ROUTES } from './modules/ranking/ranking.routes';
import { RankingModule } from './modules/ranking/ranking.module';

export const APP_ROUTES: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'synchronice-credentials', component: SynchronizeCredentialsComponent },
  { path: 'universe-selection', component: UniverseSelectionComponent, canActivate: [LoginSessionService] },
  { path: 'home', component: GameIndexComponent, canActivate: [LoginSessionService] },
  { path: 'upgrades', component: UpgradesComponent, canActivate: [LoginSessionService] },
  { path: 'units', component: UnitsComponent, canActivate: [LoginSessionService] },
  { path: 'units/build', component: UnitsComponent, canActivate: [LoginSessionService] },
  { path: 'units/deployed', component: UnitsComponent, canActivate: [LoginSessionService] },
  { path: 'units/requirements', component: UnitsComponent, canActivate: [LoginSessionService] },
  { path: 'navigate', component: NavigationComponent, canActivate: [LoginSessionService] },
  { path: 'reports', component: ReportsListComponent, canActivate: [LoginSessionService] },
  { path: 'version', component: VersionInformationComponent, canActivate: [LoginSessionService] },
  {
    path: 'alliance', component: RouterRootComponent, canActivate: [LoginSessionService],
    children: ALLIANCE_ROUTES, data: {
      sectionTitle: 'Alliance', routes: [
        { path: 'my', text: 'APP.MY_ALLIANCE' },
        { path: 'browse', text: 'APP.BROWSE' },
        { path: 'join-request', text: 'APP.LIST_JOIN_REQUEST' }
      ]
    }
  },
  {
    path: 'ranking', component: RouterRootComponent, canActivate: [LoginSessionService],
    children: RANKING_ROUTES
  },
  { path: '**', component: PageNotFoundComponent }
];

export function findHttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http);
}

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    PageNotFoundComponent,
    UniverseSelectionComponent,
    DisplaySingleUniverseComponent,
    GameIndexComponent,
    SideBarComponent,
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
    NavigationComponent,
    NavigationControlsComponent,
    DisplayQuadrantComponent,
    PlanetDisplayNamePipe,
    DeployedUnitsListComponent,
    ReportsListComponent,
    UnitsAliveDeathListComponent,
    ListRunningMissionsComponent,
    DisplayUsernamePipe,
    DisplayMissionTypePipe,
    SynchronizeCredentialsComponent,
    VersionInformationComponent,
    UnitRequirementsComponent,
    CountdownComponent,
    MilisToDatePipe,
    PlanetSelectorComponent,
    MissionModalComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    Angular2FontawesomeModule,
    RouterModule.forRoot(APP_ROUTES),
    HttpClientModule,
    NgbModule.forRoot(),
    UserModule.forRoot(),
    AllianceModule.forRoot(),
    CoreModule.forRoot(),
    UniverseModule.forRoot(),
    RankingModule,
    WidgetsModule,
    TranslateModule.forRoot({
      useDefaultLang: true,
      loader: {
        provide: TranslateLoader,
        useFactory: findHttpLoaderFactory,
        deps: [HttpClient]
      }
    })
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
    LoadingService,
    ReportService,
    SanitizeService,
    UnitTypeService,
    UpgradeTypeService,
    MissionService
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  constructor(
    private _injector: Injector,
    private _websocketService: WebsocketService,
    private _translateService: TranslateService
  ) {
    ServiceLocator.injector = this._injector;
    window['globalShit'] = this._websocketService;
    this._websocketService.addEventHandler(new PingWebsocketApplicationHandler());
    this._websocketService.initSocket('http://127.0.0.1:3000');
    this._translateService.setDefaultLang('en');
  }
}
