import { PlanetPojo } from './../shared-pojo/planet.pojo';
import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-display-single-planet',
  templateUrl: './display-single-planet.component.html',
  styleUrls: ['./display-single-planet.component.less']
})
export class DisplaySinglePlanetComponent implements OnInit {

  private _planet: PlanetPojo;
  private _planetImage: string;

  @Input()
  set planet(planet: PlanetPojo){
    if(planet){
      this._planet = planet;
      this._planetImage = PlanetPojo.findImage(planet);
    }
  }

  get planet(): PlanetPojo{
    return this._planet;
  }

  get planetImage(): string{
    return this._planetImage;
  }

  constructor() { }

  ngOnInit() {

  }

}
