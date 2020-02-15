import { Component, Input, ElementRef, ViewChild, ViewChildren, QueryList, Output, EventEmitter, OnInit } from '@angular/core';
import { ModalComponent } from '@owge/core';

import { PlanetPojo } from '../../shared-pojo/planet.pojo';
import { BaseComponent } from '../../base/base.component';
import { UnitRunningMission } from '../../shared/types/unit-running-mission.type';
import { MissionService } from '../../services/mission.service';
import { MissionInformationStore } from '../../store/mission-information.store';
import { UnitService } from '../../service/unit.service';

declare const $;
@Component({
  selector: 'app-list-running-missions',
  templateUrl: './list-running-missions.component.html',
  styleUrls: ['./list-running-missions.component.less'],
  providers: [MissionInformationStore]
})
export class ListRunningMissionsComponent extends BaseComponent implements OnInit {

  @Input()
  public runningUnitMissions: UnitRunningMission[];

  @Input()
  public displayUser: boolean;

  @Input()
  public isCancellable = false;

  /**
   * When the mission is of type <B>DEPLOYED</B> and you want to be able to move the unit to other planet
   *
   * @memberof ListRunningMissionsComponent
   */
  @Input()
  public isMovable = false;

  @Output()
  public missionDone: EventEmitter<void> = new EventEmitter();

  @ViewChild('tooltipPlanet', { read: ElementRef, static: false })
  public tooltipPlanetComponent: ElementRef;

  public tooltipPlanet: PlanetPojo;
  public isDisplayingModal = false;

  @ViewChild('navigationModal', { static: true })
  private _navigationModal: ModalComponent;

  @ViewChildren('missionRoot')
  private _components: QueryList<ElementRef>;

  public constructor(
    private _missionService: MissionService,
    private _missionInformationStore: MissionInformationStore,
    private _unitService: UnitService
  ) {
    super();
  }

  public ngOnInit(): void {
    this._missionInformationStore.missionSent.subscribe(() => {
      this._navigationModal.hide();
      this.isDisplayingModal = false;
      this.missionDone.emit();
    });
  }

  public onMouseEnter(planet: PlanetPojo): void {
    this.tooltipPlanet = planet;
    $(this.tooltipPlanetComponent.nativeElement).detach().appendTo('.tooltip');
  }

  public async cancelMission(runningUnitMissions: UnitRunningMission): Promise<void> {
    if (await this.displayConfirm('Are you sure you want to cancel the mission?')) {
      if (runningUnitMissions.type === 'DEPLOYED') {
        await this._doWithLoading(this._missionService.sendMission(
          'DEPLOY',
          runningUnitMissions.targetPlanet,
          runningUnitMissions.sourcePlanet,
          this._unitService.obtainedUnitToSelectedUnits(runningUnitMissions.involvedUnits)
        ));
      } else {
        await this._doWithLoading(this._missionService.cancelMission(runningUnitMissions.missionId).toPromise());
      }

      setTimeout(() => {
        this.missionDone.emit();
      }, 500);
    }
  }

  public moveUnit(runningMission: UnitRunningMission): void {
    this._missionInformationStore.originPlanet.next(runningMission.targetPlanet);
    this._missionInformationStore.availableUnits.next(runningMission.involvedUnits);
    this._navigationModal.show();
    this.isDisplayingModal = true;
  }
}
