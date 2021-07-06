import { Component, EventEmitter, Input, Output } from '@angular/core';
import { PlanetPojo } from './../shared-pojo/planet.pojo';

@Component({
  selector: 'app-display-single-planet',
  templateUrl: './display-single-planet.component.html',
  styleUrls: ['./display-single-planet.component.scss']
})
export class DisplaySinglePlanetComponent {

  @Output() clicked: EventEmitter<void> = new EventEmitter;

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

  public clickPlanet(): void {
    this.clicked.emit();
  }


}
