import { Component, EventEmitter, OnInit, Output, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { PlanetService } from '@owge/galaxy';
import { MenuRoute, ROUTES, ModalComponent, SessionService, LoggerHelper } from '@owge/core';
import { UserWithFaction } from '@owge/faction';
import { DisplayService, AbstractSidebarComponent } from '@owge/widgets';
import { UnitType, MissionStore, ResourceManagerService, AutoUpdatedResources, Planet, UniverseGameService } from '@owge/universe';

import { version } from '../../../version';
import { UnitTypeService } from '../../services/unit-type.service';
import { ReportService } from '../../services/report.service';
import { TwitchState } from '../../types/twitch-state.type';

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
  styleUrls: [
    './game-sidebar.component.less',
    './game-sidebar.component.scss'
  ]
})
export class GameSidebarComponent extends AbstractSidebarComponent implements OnInit {

  private static readonly _LS_DISPLAY_TWITCH_KEY = 'do_display_twitch';
  private static readonly _LS_ASK_IS_OTHER_PLAYING_UNMUTED = 'ask_is_other_playing_twitch';
  private static readonly _LS_YEAH_IS_PLAYING = 'yeah_is_playing';
  private static readonly _LOG: LoggerHelper = new LoggerHelper(GameSidebarComponent.name);

  @Output() public displayTwitch: EventEmitter<TwitchState> = new EventEmitter;
  @ViewChild('planetSelectionModal', { static: true }) private _modalComponent: ModalComponent;
  public selectedPlanet: Planet;
  public myPlanets: Planet[];
  public user: UserWithFaction;
  public withLimitUnitTypes: UnitType[];
  public resources: AutoUpdatedResources;
  public hasToDisplayTwitch: boolean = !!localStorage.getItem(GameSidebarComponent._LS_DISPLAY_TWITCH_KEY);
  public isPrimaryTwitch = false;
  public twitchRoute = this._createTranslatableMenuRoute(
    'APP.MENU_TWITCH',
    () => this._clickTwitch(),
    'fab fa-twitch',
    true,
    { 'is-twitch-active': this.hasToDisplayTwitch }
  );

  public menuRoutes: MenuRoute[] = [
    this._createTranslatableMenuRoute('APP.MENU_HOME', ROUTES.GAME_INDEX, 'fa fa-home'),
    this._createTranslatableMenuRoute('APP.MENU_UPGRADES', ROUTES.UPGRADES, 'fa fa-flask'),
    this._createTranslatableMenuRoute('APP.MENU_UNITS', ROUTES.UNITS, 'fa fa-male'),
    this._createTranslatableMenuRoute('APP.MENU_NAVIGATION', ROUTES.NAVIGATE, 'fa fa-map', true),
    this._createTranslatableMenuRoute('APP.MENU_PLANET_LIST', ROUTES.PLANET_LIST, 'fas fa-list'),
    this._createTranslatableMenuRoute('APP.MENU_REPORTS', ROUTES.REPORTS, 'fa fa-envelope'),
    this._createTranslatableMenuRoute('APP.MENU_ALLIANCES', ROUTES.ALLIANCE, 'fas fa-user-friends', true),
    this._createTranslatableMenuRoute('APP.MENU_TIME_SPECIALS', '/time_specials', 'fa fa-clock'),
    this._createTranslatableMenuRoute('APP.MENU_RANKING', ROUTES.RANKING, 'fa fa-trophy', true),
    this._createTranslatableMenuRoute('APP.MENU_SETTINGS', ROUTES.SETTINGS, 'fas fa-cog'),
    {
      text: version,
      path: ROUTES.VERSION,
      icon: 'fa fa-info'
    },
    this.twitchRoute,
    this._createTranslatableMenuRoute('APP.MENU_LOGOUT', () => this._universeGameService.logout(), 'fa fa-times')
  ];
  public missionsCount: number;
  public maxMissions: number;

  public userUnreadReports = 0;
  public enemyUnreadReports = 0;

  constructor(
    _translateService: TranslateService,
    private _universeGameService: UniverseGameService,
    private _planetService: PlanetService,
    private _displayService: DisplayService,
    private _resourceManagerService: ResourceManagerService,
    private _unitTypeService: UnitTypeService,
    private _missionStore: MissionStore,
    private _reportService: ReportService
  ) {
    super(_translateService);
  }

  public async ngOnInit() {
    window.addEventListener('storage', e => {
      if (e.key === GameSidebarComponent._LS_ASK_IS_OTHER_PLAYING_UNMUTED && e.newValue === '1') {
        localStorage.setItem(GameSidebarComponent._LS_ASK_IS_OTHER_PLAYING_UNMUTED, '0');
        localStorage.setItem(GameSidebarComponent._LS_YEAH_IS_PLAYING, this.isPrimaryTwitch ? '1' : '0');
      }
    });
    this._planetService.findMyPlanets().subscribe(planets => this.myPlanets = planets);
    this._universeGameService.findLoggedInUserData<UserWithFaction>().subscribe(user => this.user = user);
    this._unitTypeService.getUnitTypes().subscribe(unitTypes => this.withLimitUnitTypes = unitTypes.filter(current => current.maxCount));
    this.resources = new AutoUpdatedResources(this._resourceManagerService);
    this._planetService.findCurrentPlanet().subscribe(selectedPlanet => {
      this.selectedPlanet = selectedPlanet;
    });
    this._missionStore.missionsCount.subscribe(count => this.missionsCount = count);
    this._missionStore.maxMissions.subscribe(maxCount => this.maxMissions = maxCount);
    this._reportService.findUserUnreadCount().subscribe(result => this.userUnreadReports = result);
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
}
