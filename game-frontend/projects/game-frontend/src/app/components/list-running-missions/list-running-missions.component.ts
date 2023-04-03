import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { ModalComponent } from '@owge/core';
import { BaseComponent } from '../../base/base.component';
import { UnitService } from '../../service/unit.service';
import { MissionService } from '@owge/universe';
import { PlanetPojo } from '../../shared-pojo/planet.pojo';
import { UnitRunningMission } from '../../shared/types/unit-running-mission.type';
import { MissionInformationStore } from '../../store/mission-information.store';

@Component({
  selector: 'app-list-running-missions',
  templateUrl: './list-running-missions.component.html',
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

  @ViewChild('navigationModal', { static: true })
  private _navigationModal: ModalComponent;

  public tooltipPlanet: PlanetPojo;
  public isDisplayingModal = false;


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
