import { Component, OnInit, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { PlanetService } from '@owge/galaxy';
import { MenuRoute, ROUTES, ModalComponent, SessionService } from '@owge/core';
import { UserWithFaction } from '@owge/faction';
import { DisplayService, AbstractSidebarComponent } from '@owge/widgets';
import { UnitType, MissionStore, ResourceManagerService, AutoUpdatedResources, Planet, UniverseGameService } from '@owge/universe';

import { version } from '../../../version';
import { UnitTypeService } from '../../services/unit-type.service';
import { LoginSessionService } from '../../login-session/login-session.service';
import { ReportService } from '../../services/report.service';

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

  @ViewChild('planetSelectionModal', { static: true }) private _modalComponent: ModalComponent;
  public selectedPlanet: Planet;
  public myPlanets: Planet[];
  public user: UserWithFaction;
  public withLimitUnitTypes: UnitType[];
  public resources: AutoUpdatedResources;
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
    {
      text: version,
      path: ROUTES.VERSION,
      icon: 'fa fa-info'
    },
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

  public ngOnInit() {
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
}
