import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Injector, NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule, Routes } from '@angular/router';
import { ServiceWorkerModule } from '@angular/service-worker';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { AllianceModule, ALLIANCE_ROUTES, ALLIANCE_ROUTES_DATA } from '@owge/alliance';
import {
  CoreModule, LoadingService, OwgeUserModule, RouterRootComponent,
  SessionService, ThemeService, User, WarningWebsocketApplicationHandlerService
} from '@owge/core';
import { OwgeGalaxyModule, PlanetListService, PlanetService } from '@owge/galaxy';
import {
  ErrorLoggingService, OwgeUniverseModule, SystemMessageService, TimeSpecialService, UniverseGameService,
  UserStorage, WebsocketService, WsEventCacheService
} from '@owge/universe';
import { OwgeWidgetsModule } from '@owge/widgets';
import { Level, Log } from 'ng2-logger/browser';
import { take } from 'rxjs/operators';
import { environment } from '../environments/environment';
import { AppComponent } from './app.component';
import { BuildUnitsComponent } from './build-units/build-units.component';
import { PingWebsocketApplicationHandler } from './class/ping-websocket-application-handler';
import { CountdownComponent } from './components/countdown/countdown.component';
import { DeployedUnitsListComponent } from './components/deployed-units-list/deployed-units-list.component';
import { DisplayQuadrantComponent } from './components/display-quadrant/display-quadrant.component';
import { FastExplorationButtonComponent } from './components/fast-exploration-button/fast-exploration-button.component';
import { GameSidebarComponent } from './components/game-sidebar/game-sidebar.component';
import { ListRunningMissionsComponent } from './components/list-running-missions/list-running-missions.component';
import { NavigationControlsComponent } from './components/navigation-controls/navigation-controls.component';
import { NavigationComponent } from './components/navigation/navigation.component';
import { PlanetListComponent } from './components/planet-list/planet-list.component';
import { PlanetSelectorComponent } from './components/planet-selector/planet-selector.component';
import { ReportsListComponent } from './components/reports-list/reports-list.component';
import { SettingsComponent } from './components/settings/settings.component';
import { SponsorsComponent } from './components/sponsors/sponsors.component';
import { SynchronizeCredentialsComponent } from './components/synchronize-credentials/synchronize-credentials.component';
import { SystemMessagesComponent } from './components/system-messages/system-messages.component';
import { TimeSpecialsComponent } from './components/time-specials/time-specials.component';
import { TutorialOverlayComponent } from './components/tutorial-overlay/tutorial-overlay.component';
import { UnitRequirementsComponent } from './components/unit-requirements/unit-requirements.component';
import { UnitsAliveDeathListComponent } from './components/units-alive-death-list/units-alive-death-list.component';
import { VersionInformationComponent } from './components/version-information/version-information.component';
import { DeployedUnitsBigComponent } from './deployed-units-big/deployed-units-big.component';
import { DisplayDynamicImageComponent } from './display-dynamic-image/display-dynamic-image.component';
import { DisplayRequirementsComponent } from './display-requirements/display-requirements.component';
import { DisplaySingleFactionComponent } from './display-single-faction/display-single-faction.component';
import { DisplaySinglePlanetComponent } from './display-single-planet/display-single-planet.component';
import { DisplaySingleResourceComponent } from './display-single-resource/display-single-resource.component';
import { DisplaySingleUnitComponent } from './display-single-unit/display-single-unit.component';
import { DisplaySingleUniverseComponent } from './display-single-universe/display-single-universe.component';
import { DisplaySingleUpgradeComponent } from './display-single-upgrade/display-single-upgrade.component';
import { FactionSelectorComponent } from './faction-selector/faction-selector.component';
import { GameIndexComponent } from './game-index/game-index.component';
import { LoginSessionService } from './login-session/login-session.service';
import { LoginComponent } from './login/login.component';
import { MissionModalComponent } from './mission-modal/mission-modal.component';
import { ConfigurationModule } from './modules/configuration/configuration.module';
import { ConfigurationService } from './modules/configuration/services/configuration.service';
import { RankingModule } from './modules/ranking/ranking.module';
import { RANKING_ROUTES } from './modules/ranking/ranking.routes';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';
import { DisplayMissionTypePipe } from './pipes/display-mission-type.pipe';
import { DisplayUsernamePipe } from './pipes/display-username.pipe';
import { MilisToDatePipe } from './pipes/milis-to-date/milis-to-date.pipe';
import { ServiceLocator } from './service-locator/service-locator';
import { NavigationService } from './service/navigation.service';
import { UnitService } from './service/unit.service';
import { UpgradeService } from './service/upgrade.service';
import { ReportService } from './services/report.service';
import { SanitizeService } from './services/sanitize.service';
import { TwitchService } from './services/twitch.service';
import { UpgradeTypeService } from './services/upgrade-type.service';
import { UnitsComponent } from './units/units.component';
import { UniverseSelectionComponent } from './universe-selection/universe-selection.component';
import { UpgradesComponent } from './upgrades/upgrades.component';
import {PlanetToNavigationPipe} from './pipes/planet-to-navigation.pipe';

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
  { path: 'planet-list', component: PlanetListComponent, canActivate: [LoginSessionService] },
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
  {
    path: 'settings', component: SettingsComponent, canActivate: [LoginSessionService]
  },
  {
    path: 'system-messages', component: SystemMessagesComponent, canActivate: [LoginSessionService]
  },
  {
    path: 'sponsors', component: SponsorsComponent, canActivate: [LoginSessionService]
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
    PlanetListComponent,
    SettingsComponent,
    TutorialOverlayComponent,
    FastExplorationButtonComponent,
    SystemMessagesComponent,
    SponsorsComponent,
    PlanetToNavigationPipe
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    RouterModule.forRoot(APP_ROUTES, { onSameUrlNavigation: 'reload', initialNavigation: 'enabled' }),
    HttpClientModule,
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
    ServiceWorkerModule.register('ngsw-worker.js', { enabled: environment.production, registrationStrategy: 'registerImmediately' })
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
    UpgradeTypeService,
    TwitchService,
    SystemMessageService,
    ThemeService
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  constructor(
    private _injector: Injector,
    private _websocketService: WebsocketService,
    private _translateService: TranslateService,
    private _configurationService: ConfigurationService,
    private _userStorage: UserStorage<User>,
    private _universeGameService: UniverseGameService,
    private _wsEventCacheService: WsEventCacheService,
    private themeService: ThemeService,
    private errorLoggingService: ErrorLoggingService
  ) {
    ServiceLocator.injector = this._injector;
    themeService.useUserDefinedOrDefault();
    this._initWebsocket();
    (window as any).owgeDebug = (isEnabled => {
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
    this._wsEventCacheService.addCacheListeners(this._configurationService);
    this._configurationService.observeParamOrDefault('WEBSOCKET_ENDPOINT', '/websocket/socket.io')
      .subscribe(conf => {
        this._websocketService.addEventHandler(
          new PingWebsocketApplicationHandler(this.themeService),
          this._injector.get(UpgradeService),
          this._injector.get(UnitService),
          this._injector.get(PlanetService),
          this._injector.get(ReportService),
          this._injector.get(TimeSpecialService),
          this._injector.get(UpgradeTypeService),
          this._injector.get(PlanetListService),
          this._injector.get(TwitchService),
          this._injector.get(SystemMessageService),
          this._injector.get(WarningWebsocketApplicationHandlerService),
          this._injector.get(SessionService)
        );
        this._universeGameService.isInGame().subscribe(async isInGame => {
          const token = await this._userStorage.currentToken.pipe(take(1)).toPromise();
          if (isInGame) {
            this._websocketService.initSocket(conf.value, token);
          } else {
            this._websocketService.close();
          }
        });
      });
  }
}

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
export function findHttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http, './assets/i18n/');
}
