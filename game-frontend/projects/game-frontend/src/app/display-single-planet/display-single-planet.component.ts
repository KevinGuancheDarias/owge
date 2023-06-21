import { Component, EventEmitter, Input, Output } from '@angular/core';
import {Planet} from '@owge/universe';
import {NavigationConfig} from '../shared/types/navigation-config.type';
import {PlanetUtil} from '../shared/util/planet.util';

@Component({
  selector: 'app-display-single-planet',
  templateUrl: './display-single-planet.component.html',
  styleUrls: ['./display-single-planet.component.scss']
})
export class DisplaySinglePlanetComponent {

  @Output() clicked: EventEmitter<void> = new EventEmitter;

  navigationToPlanet: NavigationConfig;

  private _planet: Planet;

  @Input()
  set planet(planet: Planet) {
    if (planet) {
      this._planet = planet;
      this.navigationToPlanet = PlanetUtil.planetToNavigationConfig(planet);
    }
  }

  get planet(): Planet {
    return this._planet;
  }

  public clickPlanet(): void {
    this.clicked.emit();
  }
}
