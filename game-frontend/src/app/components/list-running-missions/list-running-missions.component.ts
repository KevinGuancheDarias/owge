import { Component, Input, ElementRef, ViewChild, ViewChildren, QueryList } from '@angular/core';
import { AnyRunningMission } from '../../shared/types/any-running-mission.type';
import { PlanetPojo } from '../../shared-pojo/planet.pojo';
import { BaseComponent } from '../../base/base.component';
import { UnitRunningMission } from '../../shared/types/unit-running-mission.type';

declare const $;
@Component({
  selector: 'app-list-running-missions',
  templateUrl: './list-running-missions.component.html',
  styleUrls: ['./list-running-missions.component.less']
})
export class ListRunningMissionsComponent extends BaseComponent {

  @Input()
  public runningUnitMissions: UnitRunningMission[];

  @Input()
  public displayUser: boolean;

  @ViewChild('tooltipPlanet', { read: ElementRef })
  public tooltipPlanetComponent: ElementRef;

  @ViewChildren('missionRoot')
  private _components: QueryList<ElementRef>;

  public tooltipPlanet: PlanetPojo;

  public onReady(): void {
    this.autoSpanCard(this._components, '.auto-expand', el => el.parentElement.parentElement.parentElement);
  }

  public onMouseEnter(planet: PlanetPojo): void {
    this.tooltipPlanet = planet;
    $(this.tooltipPlanetComponent.nativeElement).detach().appendTo('.tooltip');
  }
}
