import { animate, state, style, transition, trigger } from '@angular/animations';
import { AfterViewInit, Component, ElementRef, EventEmitter, OnInit, Output, ViewChild, ViewEncapsulation } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { LoggerHelper, MenuRoute, ModalComponent, ROUTES, ThemeService, UnitType } from '@owge/core';
import { UserWithFaction } from '@owge/faction';
import { PlanetService } from '@owge/galaxy';
import {
  AutoUpdatedResources, MissionStore,
  Planet, ResourceManagerService,
  SystemMessageService, UnitBuildRunningMission, UniverseGameService, UpgradeRunningMission
} from '@owge/universe';
import { AbstractSidebarComponent, DisplayService, WidgetConfirmationDialogComponent } from '@owge/widgets';
import { combineLatest, EMPTY, fromEvent, Observable, Subscription } from 'rxjs';
import { delay, map, startWith, take } from 'rxjs/operators';
import { version } from '../../../version';
import { ConfigurationService } from '../../modules/configuration/services/configuration.service';
import { UnitService } from '../../service/unit.service';
import { UpgradeService } from '../../service/upgrade.service';
import { ReportService } from '../../services/report.service';
import { TwitchService } from '../../services/twitch.service';
import { UnitTypeService } from '../../services/unit-type.service';
import { TwitchState } from '../../types/twitch-state.type';

class PlanetWrapper {
  constructor(public planet: Planet, public buildingMission$: Observable<UnitBuildRunningMission>) {

  }
}

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Component({
  selector: 'app-game-sidebar',
  templateUrl: './game-sidebar.component.html',
  animations: [
    trigger('goRight', [
      state('left', style({
        left: '20px',
        top: '-7px',
        fontSize: '15pt'
      })),
      state('right', style({
        left: '25px',
        top: '-11px',
        fontSize: '18pt',
      })),
      transition('left => right', animate(900)),
      transition('right => left', animate(900))
    ])
  ],
  encapsulation: ViewEncapsulation.None
})
export class GameSidebarComponent extends AbstractSidebarComponent implements OnInit, AfterViewInit {

  private static readonly _LS_DISPLAY_TWITCH_KEY = 'do_display_twitch';
  private static readonly _LS_ASK_IS_OTHER_PLAYING_UNMUTED = 'ask_is_other_playing_twitch';
  private static readonly _LS_YEAH_IS_PLAYING = 'yeah_is_playing';
  private static readonly _LOG: LoggerHelper = new LoggerHelper(GameSidebarComponent.name);

  @Output() public displayTwitch: EventEmitter<TwitchState> = new EventEmitter;
  @ViewChild('cancelUnitConfirmDialog') public cancelUnitConfirmDialog: WidgetConfirmationDialogComponent;
  @ViewChild('cancelUpgradeConfirmDialog') public cancelUpgradeConfirmDialog: WidgetConfirmationDialogComponent;
  @ViewChild('planetSelectionModal', { static: true }) private _modalComponent: ModalComponent;
  public selectedPlanet: Planet;
  public myPlanets: PlanetWrapper[];
  public user: UserWithFaction;
  public withLimitUnitTypes: UnitType[];
  public resources: AutoUpdatedResources;
  public hasToDisplayTwitch = !!localStorage.getItem(GameSidebarComponent._LS_DISPLAY_TWITCH_KEY);
  public isPrimaryTwitch = false;
  public twitchRoute = this._createTranslatableMenuRoute(
    'APP.MENU_TWITCH',
    () => this._clickTwitch(),
    'fab fa-twitch',
    true,
    { 'is-twitch-active': this.hasToDisplayTwitch }
  );

  public animationState: 'left' | 'right' = 'left';

  public menuRoutes: MenuRoute[] = [
    this._createTranslatableMenuRoute('APP.MENU_HOME', ROUTES.GAME_INDEX, 'fa fa-home'),
    this._createTranslatableMenuRoute('APP.MENU_UPGRADES', ROUTES.UPGRADES, 'fa fa-flask'),
    this._createTranslatableMenuRoute('APP.MENU_UNITS', ROUTES.UNITS, 'fa fa-male'),
    this._createTranslatableMenuRoute('APP.MENU_NAVIGATION', ROUTES.NAVIGATE, 'fa fa-map', true),
    this._createTranslatableMenuRoute('APP.MENU_PLANET_LIST', ROUTES.PLANET_LIST, 'fas fa-list'),
    this._createTranslatableMenuRoute('APP.MENU_REPORTS', ROUTES.REPORTS, 'fa fa-envelope'),
    this._createTranslatableMenuRoute('APP.MENU_ALLIANCES', ROUTES.ALLIANCE, 'fas fa-user-friends', true),
    this._createTranslatableMenuRoute('APP.MENU_TIME_SPECIALS', '/time_specials', 'fas fa-arrow-down'),
    this._createTranslatableMenuRoute('APP.MENU_RANKING', ROUTES.RANKING, 'fa fa-trophy', true),
    this._createTranslatableMenuRoute('APP.MENU_SETTINGS', ROUTES.SETTINGS, 'fas fa-cog'),
    {
      text: version,
      path: ROUTES.VERSION,
      icon: 'fa fa-info'
    },
    this.twitchRoute,
    this._createTranslatableMenuRoute('APP.MENU_SYSTEM_MESSAGES', ROUTES.SYSTEM_MESSAGES, 'fa fa-robot'),
    this._createTranslatableMenuRoute('APP.MENU_SPONSORS', ROUTES.SPONSORS, 'fas fa-heart', true),
    this._createTranslatableMenuRoute('APP.MENU_LOGOUT', () => this._universeGameService.logout(), 'fa fa-times')
  ];
  public missionsCount: number;
  public maxMissions: number;

  public userUnreadReports = 0;
  public enemyUnreadReports = 0;
  public isTwitchLive = false;
  public unreadSystemMessages = 0;
  public invisibleHeight: number;
  public runningUpgrade$: Observable<UpgradeRunningMission> = EMPTY;
  public renderUpgradeDetails = false;

  private neonThemeSubscription: Subscription;
  private building: UnitBuildRunningMission;

  constructor(
    private _universeGameService: UniverseGameService,
    private _planetService: PlanetService,
    private _displayService: DisplayService,
    private _resourceManagerService: ResourceManagerService,
    private _unitTypeService: UnitTypeService,
    private _missionStore: MissionStore,
    private _reportService: ReportService,
    private _systemMessageService: SystemMessageService,
    private _configurationService: ConfigurationService,
    private _el: ElementRef<HTMLElement>,
    private themeService: ThemeService,
    private unitService: UnitService,
    private upgradeService: UpgradeService,
    twitchService: TwitchService,
    translateService: TranslateService,
  ) {
    super(translateService);
    twitchService.state().subscribe(value => {
      this.twitchRoute.cssClasses['is-twitch-live'] = value;
      this.isTwitchLive = value;
    });
  }

  public async ngOnInit() {
    this.subscribeMyPlanets();
    window.addEventListener('storage', e => {
      if (e.key === GameSidebarComponent._LS_ASK_IS_OTHER_PLAYING_UNMUTED && e.newValue === '1') {
        localStorage.setItem(GameSidebarComponent._LS_ASK_IS_OTHER_PLAYING_UNMUTED, '0');
        localStorage.setItem(GameSidebarComponent._LS_YEAH_IS_PLAYING, this.isPrimaryTwitch ? '1' : '0');
      }
    });
    this._universeGameService.findLoggedInUserData<UserWithFaction>().subscribe(user => this.user = user);
    this._unitTypeService.getUnitTypes().subscribe(unitTypes => this.withLimitUnitTypes = unitTypes.filter(current => current.maxCount));
    this.resources = new AutoUpdatedResources(this._resourceManagerService);
    this._planetService.findCurrentPlanet().subscribe(selectedPlanet => {
      this.selectedPlanet = selectedPlanet;
    });
    this._missionStore.missionsCount.subscribe(count => this.missionsCount = count);
    this._missionStore.maxMissions.subscribe(maxCount => this.maxMissions = maxCount);
    this._reportService.findUserUnreadCount().subscribe(result => this.userUnreadReports = result);
    this._systemMessageService.findAll().pipe(
      map(result => result.filter(message => !message.isRead).length)
    ).subscribe(systemUnread => this.unreadSystemMessages = systemUnread);
    this._reportService.findEnemyUnreadCount().subscribe(result => this.enemyUnreadReports = result);
    window.addEventListener('storage', e => {
      if (e.key === GameSidebarComponent._LS_DISPLAY_TWITCH_KEY) {
        this._syncTwitchButton(false);
      }
    });
    this.isPrimaryTwitch = !(await this._isTwitchPlayingUnmutedInOtherTab());
    this.displayTwitch.emit({
      hasToDisplay: this.hasToDisplayTwitch,
      isPrimary: this.isPrimaryTwitch
    });
  }

  public ngAfterViewInit(): void {
    this._handleShouldDisplay();
    window.setTimeout(() => this._handleNeon(), 500);
  }

  public cancelBuild(confirm: boolean): void {
    if (confirm) {
      this.unitService.cancel(this.building).pipe(take(1)).subscribe();
    }
  }

  public cancelUpgrade(confirm: boolean): void {
    if(confirm) {
      this.upgradeService.cancelUpgrade();
    }
  }

  public displayPlanetSelectionModal(): void {
    this._modalComponent.show();
  }

  public selectPlanet(planet: Planet): void {
    this._planetService.defineSelectedPlanet(planet);
  }

  public async leavePlanet(planet: Planet): Promise<void> {
    if (await this._displayService.confirm('Leave the planet ' + planet.name + '?')) {
      this._planetService.leavePlanet(planet).subscribe(() => { });
    }
  }

  public toggleStateAnimation(): void {
    this.animationState = this.animationState === 'left' ? 'right' : 'left';
  }

  public showCancel(building: UnitBuildRunningMission) {
    this.building = building;
    this.cancelUnitConfirmDialog.show();
  }

  public showCancelUpgrade() {
    this.cancelUpgradeConfirmDialog.show();
  }

  public cancelRender(): void {
    console.log('cancel');
    window.setTimeout(() => this.renderUpgradeDetails = false, 100);
  }

  private subscribeMyPlanets() {
    this._planetService.findMyPlanets().subscribe(planets => {
      this.myPlanets = planets.map(planet => new PlanetWrapper(planet, this.unitService.findBuildingMissionInMyPlanet(planet.id)));
    });
  }

  private _clickTwitch(): void {
    this.hasToDisplayTwitch = !this.hasToDisplayTwitch;
    if (!this.hasToDisplayTwitch) {
      localStorage.removeItem(GameSidebarComponent._LS_DISPLAY_TWITCH_KEY);
    } else {
      localStorage.setItem(GameSidebarComponent._LS_DISPLAY_TWITCH_KEY, 'true');
    }
    this._syncTwitchButton(true);
  }

  private _syncTwitchButton(isPrimary: boolean): void {
    this.hasToDisplayTwitch = !!localStorage.getItem(GameSidebarComponent._LS_DISPLAY_TWITCH_KEY);
    this.twitchRoute.cssClasses = { 'is-twitch-active': this.hasToDisplayTwitch };
    this.displayTwitch.emit({
      hasToDisplay: this.hasToDisplayTwitch,
      isPrimary
    });
  }

  private async _isTwitchPlayingUnmutedInOtherTab(): Promise<boolean> {
    let handlerFromOutside;
    const promise1 = new Promise<boolean>(resolve => window.setTimeout(() => {
      window.removeEventListener('storage', handlerFromOutside);
      localStorage.setItem(GameSidebarComponent._LS_ASK_IS_OTHER_PLAYING_UNMUTED, '0');
      resolve(false);
    }, 2000));
    const promise2 = new Promise<boolean>(resolve => {
      const handler = (e: StorageEvent) => {
        if (e.key === GameSidebarComponent._LS_YEAH_IS_PLAYING && e.newValue === '1') {
          window.removeEventListener('storage', handler);
          localStorage.setItem(GameSidebarComponent._LS_YEAH_IS_PLAYING, '0');
          GameSidebarComponent._LOG.debug('Twitch is playing from other tab');
          resolve(true);
        }
      };
      handlerFromOutside = handler;
      window.addEventListener('storage', handler);
    });
    localStorage.setItem(GameSidebarComponent._LS_ASK_IS_OTHER_PLAYING_UNMUTED, '1');
    const retVal: boolean = await Promise.race([promise1, promise2]);
    if (!retVal) {
      GameSidebarComponent._LOG.debug('Twitch is playing as primary in this tab');
    }
    return retVal;
  }

  private _handleShouldDisplay(): void {
    const alliancesRoute: MenuRoute = this.menuRoutes[6];
    this._configurationService.observeParamOrDefault('DISABLED_FEATURE_ALLIANCE', 'FALSE')
      .pipe(
        map(configuration => configuration.value === 'TRUE')
      ).subscribe(isDisabled => {
        alliancesRoute.shouldDisplay = !isDisabled;
      });
  }

  private _handleNeon(): void {
    this.themeService.currentTheme$.subscribe(theme => {
      if(theme === 'neon') {
        const sidebar: HTMLElement = this._el.nativeElement.querySelector('.owge-side-bar-root');
        this.neonThemeSubscription = combineLatest(
          fromEvent(window, 'resize').pipe(startWith(null)),
          fromEvent(document, 'scroll').pipe(startWith(null)),
          this._planetService.findMyPlanets().pipe(startWith(null))
        ).pipe(delay(300))
        .subscribe(() => {
          const classList: DOMTokenList = document.body.classList;
          if(classList.contains('owge-screen-desktop')) {
            this.invisibleHeight = sidebar.offsetHeight;
          } else {
            this.invisibleHeight = 0;
          }
        });

        this.runningUpgrade$ = this.upgradeService.findRunningLevelUp();
      } else if(this.neonThemeSubscription) {
        this.neonThemeSubscription.unsubscribe();
        delete this.neonThemeSubscription;
        this.runningUpgrade$ = EMPTY;
      }
    });
  }
}
