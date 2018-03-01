import { BaseComponent } from './../base/base.component';
import { ROUTES } from './../config/config.pojo';
import { PlanetPojo } from './../shared-pojo/planet.pojo';
import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-side-bar',
  templateUrl: './side-bar.component.html',
  styleUrls: ['./side-bar.component.less']
})
export class SideBarComponent extends BaseComponent implements OnInit {
  public sideBarOpen: boolean = true;
  public menuRoutes = ROUTES;

  @Input()
  /** @var {PlanetPojo} following will be  changed by other components*/
  public selectedPlanet: PlanetPojo;

  constructor() {
    super();
    this.requireUser();
  }

  public ngOnInit() {
    this.requireUser();
    this.resourcesAutoUpdate();
  }
}
