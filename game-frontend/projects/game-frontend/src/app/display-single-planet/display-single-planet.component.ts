import { PlanetPojo } from './../shared-pojo/planet.pojo';
import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-display-single-planet',
  templateUrl: './display-single-planet.component.html',
  styleUrls: [
    './display-single-planet.component.less',
    './display-single-planet.component.scss'
  ]
})
export class DisplaySinglePlanetComponent implements OnInit {

  private _planet: PlanetPojo;

  @Input()
  set planet(planet: PlanetPojo) {
    if (planet) {
      this._planet = planet;
    }
  }

  get planet(): PlanetPojo {
    return this._planet;
  }

  constructor() { }

  ngOnInit() {

  }

}
