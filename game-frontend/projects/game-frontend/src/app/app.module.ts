import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { filter, map } from 'rxjs/operators';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { Angular2FontawesomeModule } from 'angular2-fontawesome/angular2-fontawesome';
import { Injector } from '@angular/core';
import { HttpClientModule, HttpClient } from '@angular/common/http';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { RouterRootComponent, OwgeUserModule, CoreModule, LoadingService, User, UserStorage } from '@owge/core';
import { ALLIANCE_ROUTES, ALLIANCE_ROUTES_DATA, AllianceModule } from '@owge/alliance';
import { OwgeUniverseModule, WebsocketService } from '@owge/universe';
import { OwgeWidgetsModule } from '@owge/widgets';
import { OwgeGalaxyModule, PlanetService } from '@owge/galaxy';

import { environment } from '../environments/environment';
import { ServiceLocator } from './service-locator/service-locator';
import { LoginSessionService } from './login-session/login-session.service';
import { NavigationService } from './service/navigation.service';
import { UnitService } from './service/unit.service';
import { UpgradeService } from './service/upgrade.service';

import { AppComponent } from './app.component';
import { LoginComponent } from './login/login.component';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';
import { UniverseSelectionComponent } from './universe-selection/universe-selection.component';
import { DisplaySingleUniverseComponent } from './display-single-universe/display-single-universe.component';
import { GameIndexComponent } from './game-index/game-index.component';
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
import { PingWebsocketApplicationHandler } from './class/ping-websocket-application-handler';
import { DeployedUnitsListComponent } from './components/deployed-units-list/deployed-units-list.component';
import { MissionService } from './services/mission.service';
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
import { TranslateModule, TranslateLoader, TranslateService } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { RANKING_ROUTES } from './modules/ranking/ranking.routes';
import { RankingModule } from './modules/ranking/ranking.module';
import { ConfigurationModule } from './modules/configuration/configuration.module';
import { ConfigurationService } from './modules/configuration/services/configuration.service';
import { Subscription } from 'rxjs';
import { GameSidebarComponent } from './components/game-sidebar/game-sidebar.component';
import { TimeSpecialsComponent } from './components/time-specials/time-specials.component';
import { TimeSpecialService } from './services/time-specials.service';
import { Log, Level } from 'ng2-logger/browser';
import { ServiceWorkerModule } from '@angular/service-worker';

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
    path: 'time_specials', component: TimeSpecialsComponent, canActivate: [LoginSessionService]
  },
  {
    path: 'alliance', component: RouterRootComponent, canActivate: [LoginSessionService],
    children: ALLIANCE_ROUTES, data: ALLIANCE_ROUTES_DATA
  },
  {
    path: 'ranking', component: RouterRootComponent, canActivate: [LoginSessionService],
    children: RANKING_ROUTES, data: {
      sectionTitle: 'Ranking'
    }
  },
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
    MissionModalComponent,
    GameSidebarComponent,
    TimeSpecialsComponent,
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    Angular2FontawesomeModule,
    RouterModule.forRoot(APP_ROUTES, { onSameUrlNavigation: 'reload', initialNavigation: true }),
    HttpClientModule,
    NgbModule,
    OwgeUserModule,
    AllianceModule.forRoot(),
    OwgeUniverseModule.forRoot(),
    ConfigurationModule.forRoot(),
    CoreModule.forRoot({
      url: environment.accountUrl,
      loginEndpoint: environment.loginEndpoint,
      loginDomain: environment.loginDomain,
      loginClientId: environment.loginClientId,
      loginClientSecret: environment.loginClientSecret,
      contextPath: 'game',
    }),
    RankingModule,
    OwgeWidgetsModule,
    OwgeGalaxyModule,
    TranslateModule.forRoot({
      useDefaultLang: true,
      loader: {
        provide: TranslateLoader,
        useFactory: findHttpLoaderFactory,
        deps: [HttpClient]
      }
    }),
    ServiceWorkerModule.register('ngsw-worker.js', { enabled: environment.production })
  ],
  providers: [
    LoginSessionService,
    UpgradeService,
    UnitService,
    NavigationService,
    WebsocketService,
    LoadingService,
    ReportService,
    SanitizeService,
    UnitTypeService,
    UpgradeTypeService,
    MissionService,
    TimeSpecialService
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  constructor(
    private _injector: Injector,
    private _websocketService: WebsocketService,
    private _translateService: TranslateService,
    private _configurationService: ConfigurationService,
    private _userStorage: UserStorage<User>
  ) {
    ServiceLocator.injector = this._injector;
    this._initWebsocket();
    ((<any>window).owgeDebug = isEnabled => {
      if (!isEnabled || environment.production) {
        Log.onlyLevel(Level.INFO, Level.WARN, Level.ERROR);
      } else if (!isEnabled && !environment.production) {
        console.log('OWGE started without debug, run owgeDebug() in devtools for debug');
      }
      localStorage.setItem('owge_debug', isEnabled);
    })(localStorage.getItem('owge_debug'));
    const supportedLanguages = ['en', 'es'];
    const browserLang = this._translateService.getBrowserLang();
    const targetLang = supportedLanguages.some(current => current === browserLang) ? browserLang : 'en';
    this._translateService.setDefaultLang(targetLang);
  }

  private _initWebsocket(): void {
    let _oldSuscription: Subscription;
    this._configurationService.observeParamOrDefault('WEBSOCKET_ENDPOINT', '/websocket/socket.io')
      .subscribe(conf => {
        if (_oldSuscription) {
          _oldSuscription.unsubscribe();
          _oldSuscription = null;
        }
        this._websocketService.addEventHandler(
          new PingWebsocketApplicationHandler(),
          this._injector.get(MissionService),
          this._injector.get(UpgradeService),
          this._injector.get(UnitService),
          this._injector.get(UnitTypeService),
          this._injector.get(PlanetService),
          this._injector.get(ReportService),
          this._injector.get(TimeSpecialService),
          this._injector.get(UpgradeTypeService)
        );
        _oldSuscription = this._userStorage.currentToken
          .pipe(filter(token => !!token))
          .subscribe(token => this._websocketService.initSocket(conf.value, token));
      });
  }
}

export function findHttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http);
}
