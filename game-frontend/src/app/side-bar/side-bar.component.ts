import { Component, OnInit, Input, ViewChild } from '@angular/core';

import { BaseComponent } from './../base/base.component';
import { ROUTES } from './../config/config.pojo';
import { PlanetPojo } from './../shared-pojo/planet.pojo';
import { PlanetService } from '../service/planet.service';
import { ModalComponent } from '../components/modal/modal.component';

@Component({
  selector: 'app-side-bar',
  templateUrl: './side-bar.component.html',
  styleUrls: ['./side-bar.component.less']
})
export class SideBarComponent extends BaseComponent implements OnInit {
  @Input()
  /** @var {PlanetPojo} selectedPlanet following will be changed by other components*/
  public selectedPlanet: PlanetPojo;

  public sideBarOpen = true;
  public menuRoutes = ROUTES;
  public myPlanets: PlanetPojo[];

  @ViewChild('planetSelectionModal')
  private _modalComponent: ModalComponent;

  constructor(private _planetService: PlanetService) {
    super();
    this.requireUser();
  }

  public ngOnInit() {
    this.requireUser();
    this.resourcesAutoUpdate();
    this._planetService.myPlanets.subscribe(planets => this.myPlanets = planets);
  }

  public displayPlanetSelectionModal(): void {
    this._modalComponent.show();
  }

  public selectPlanet(planet: PlanetPojo): void {
    this.loginSessionService.defineSelectedPlanet(planet);
  }
}
