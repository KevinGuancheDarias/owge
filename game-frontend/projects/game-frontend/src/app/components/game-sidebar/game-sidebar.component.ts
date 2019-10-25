import { Component, OnInit, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { Planet, PlanetService, PlanetStore } from '@owge/galaxy';
import { MenuRoute, ROUTES, UserStorage, ModalComponent } from '@owge/core';
import { UserWithFaction } from '@owge/faction';
import { DisplayService, AbstractSidebarComponent } from '@owge/widgets';
import { UnitType } from '@owge/universe';

import { version } from '../../../version';
import { ResourceManagerService } from '../../service/resource-manager.service';
import { UnitTypeService } from '../../services/unit-type.service';
import { AutoUpdatedResources } from '../../class/auto-updated-resources';

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
  styleUrls: ['./game-sidebar.component.less']
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
    this._createTranslatableMenuRoute('APP.MENU_NAVIGATION', ROUTES.NAVIGATE, 'fa fa-map'),
    this._createTranslatableMenuRoute('APP.MENU_REPORTS', ROUTES.REPORTS, 'fa fa-envelope'),
    this._createTranslatableMenuRoute('APP.MENU_ALLIANCES', ROUTES.ALLIANCE, 'fas fa-user-friends'),
    this._createTranslatableMenuRoute('APP.MENU_RANKING', ROUTES.RANKING, 'fa fa-trophy'),
    {
      text: version,
      path: ROUTES.VERSION,
      icon: 'fa fa-info'
    }
  ];
  constructor(
    private _translateService: TranslateService,
    private _userStorage: UserStorage<UserWithFaction>,
    private _planetService: PlanetService,
    private _displayService: DisplayService,
    private _resourceManagerService: ResourceManagerService,
    private _unitTypeService: UnitTypeService,
    private _planetStore: PlanetStore
  ) {
    super(_translateService);
  }

  public ngOnInit() {
    this._userStorage.currentUser.subscribe(user => this.user = user);
    this._unitTypeService.getUnitTypes().subscribe(unitTypes => this.withLimitUnitTypes = unitTypes.filter(current => current.maxCount));
    this.resources = new AutoUpdatedResources(this._resourceManagerService);
    this._planetStore.selectedPlanet.subscribe(selectedPlanet => {
      this.selectedPlanet = selectedPlanet;
    });
    this._loadMyPlanets();
  }

  public displayPlanetSelectionModal(): void {
    this._modalComponent.show();
  }

  public selectPlanet(planet: Planet): void {
    this._planetService.defineSelectedPlanet(planet);
  }

  public async leavePlanet(planet: Planet): Promise<void> {
    if (await this._displayService.confirm('Leave the planet ' + planet.name + '?')) {
      this._planetService.leavePlanet(planet).subscribe(
        async () => {
          await this._loadMyPlanets();
          if (this.selectedPlanet.id === planet.id) {
            this._planetService.defineSelectedPlanet(this.myPlanets.find(current => current.home));
          }
        },
        error => this._displayService.error(error)
      );
    }
  }

  private async _loadMyPlanets(): Promise<void> {
    this.myPlanets = await this._planetService.findMyPlanets();
  }
}
