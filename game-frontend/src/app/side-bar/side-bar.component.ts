import { Component, OnInit, Input, ViewChild } from '@angular/core';
import { Router } from '@angular/router';

import { BaseComponent } from './../base/base.component';
import { ROUTES } from './../config/config.pojo';
import { PlanetPojo } from './../shared-pojo/planet.pojo';
import { PlanetService } from '../service/planet.service';
import { ModalComponent } from '../components/modal/modal.component';
import { UniverseService } from '../universe/universe.service';
import { UniverseLocalConfig } from '../shared/types/universe-local-config.type';
import { UnitTypeService } from '../services/unit-type.service';
import { UnitType } from '../shared/types/unit-type.type';

const { version, sgt: { versionDate } } = require('../../../package.json');

@Component({
  selector: 'app-side-bar',
  templateUrl: './side-bar.component.html',
  styleUrls: ['./side-bar.component.less'],
  providers: [UniverseService]
})
export class SideBarComponent extends BaseComponent implements OnInit {

  @Input()
  /** @var {PlanetPojo} selectedPlanet following will be changed by other components*/
  public selectedPlanet: PlanetPojo;

  public sideBarOpen = true;
  public menuRoutes = ROUTES;
  public myPlanets: PlanetPojo[];
  public versionInformation: { version: string, versionDate: string };
  public withLimitUnitTypes: UnitType[];

  @ViewChild('planetSelectionModal')
  private _modalComponent: ModalComponent;

  constructor(
    private _planetService: PlanetService,
    private _universeService: UniverseService,
    private _router: Router,
    private _unitTypeService: UnitTypeService
  ) {
    super();
    this.requireUser();
    this.versionInformation = { version, versionDate };
  }

  public ngOnInit() {
    this.requireUser();
    this.resourcesAutoUpdate();
    this._planetService.myPlanets.subscribe(planets => this.myPlanets = planets);
    this._unitTypeService.getUnitTypes().subscribe(unitTypes => this.withLimitUnitTypes = unitTypes.filter(current => current.maxCount));
    if (this._universeService.isUpdatedVersion(version)) {
      alert(`El juego se ha actualizado a ${version}`);
      const currentConfig: UniverseLocalConfig = this._universeService.findUniverseUserLocalConfig();
      currentConfig.notifiedVersion = version;
      this._universeService.saveUniverseLocalConfig(currentConfig);
      this._router.navigate(['/version']);

    }
  }

  public displayPlanetSelectionModal(): void {
    this._modalComponent.show();
  }

  public selectPlanet(planet: PlanetPojo): void {
    this.loginSessionService.defineSelectedPlanet(planet);
  }

  public async leavePlanet(planet: PlanetPojo): Promise<void> {
    if (await this.displayConfirm('Leave the planet ' + planet.name + '?')) {
      this._planetService.leavePlanet(planet).subscribe(
        async () => {
          const selectedPlanet: PlanetPojo = this.loginSessionService.getSelectedPlanet();
          await this._planetService.findMyPlanets();
          if (selectedPlanet.id === planet.id) {
            console.log('great!');
            this.loginSessionService.defineSelectedPlanet(this.myPlanets.find(current => current.home));
          }
        },
        error => this.displayError(error)
      );
    }
  }

  public logout(): void {
    this.loginSessionService.logout();
  }
}
