import { Component, Input, ElementRef, ViewChild, ViewChildren, QueryList, Output, EventEmitter } from '@angular/core';
import { PlanetPojo } from '../../shared-pojo/planet.pojo';
import { BaseComponent } from '../../base/base.component';
import { UnitRunningMission } from '../../shared/types/unit-running-mission.type';
import { MissionService } from '../../services/mission.service';

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

  @Input()
  public isCancellable = false;

  @Output()
  public missionDone: EventEmitter<void> = new EventEmitter();

  @ViewChild('tooltipPlanet', { read: ElementRef })
  public tooltipPlanetComponent: ElementRef;

  @ViewChildren('missionRoot')
  private _components: QueryList<ElementRef>;

  public tooltipPlanet: PlanetPojo;

  public constructor(private _missionService: MissionService) {
    super();
  }

  public onReady(): void {
    this.autoSpanCard(this._components, '.auto-expand', el => el.parentElement.parentElement.parentElement);
  }

  public onMouseEnter(planet: PlanetPojo): void {
    this.tooltipPlanet = planet;
    $(this.tooltipPlanetComponent.nativeElement).detach().appendTo('.tooltip');
  }

  public convertToDate(unixTimestamp: number): Date {
    return new Date(unixTimestamp);
  }

  public async cancelMission(missionId: number): Promise<void> {
    if (await this.displayConfirm('Are you sure you want to cancel the mission?')) {
      await this._doWithLoading(this._missionService.cancelMission(missionId).toPromise());
      setTimeout(() => {
        this.missionDone.emit();
      }, 500);
    }
  }
}
