import { MEDIA_ROUTES } from '../../config/config.pojo';
import { Component, OnInit, ViewEncapsulation, Input } from '@angular/core';
import { NavigationData } from 'app/shared/types/navigation-data.type';
import { PlanetPojo } from 'app/shared-pojo/planet.pojo';

@Component({
  selector: 'app-display-quadrant',
  templateUrl: './display-quadrant.component.html',
  styleUrls: ['./display-quadrant.component.less'],
  encapsulation: ViewEncapsulation.None
})
export class DisplayQuadrantComponent {

  @Input()
  public navigationData: NavigationData;

  constructor() { }

  public findImage(planet: PlanetPojo): string {
    return PlanetPojo.findImage(planet);
  }

  public findUiIcon(name: string) {
    return MEDIA_ROUTES.UI_ICONS + name;
  }

  public showMissionDialog(): void {

  }

}
