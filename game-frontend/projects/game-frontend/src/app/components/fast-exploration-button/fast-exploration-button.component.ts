import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { MissionUtil, ToastrService } from '@owge/core';
import { MissionStore, Planet, MissionService } from '@owge/universe';
import { ObtainedUnit, UnitRunningMission } from '@owge/types/universe';
import { BaseComponent } from '../../base/base.component';
import { MissionInformationStore } from '../../store/mission-information.store';

interface DisplayQuadrantUnitRunningMission extends UnitRunningMission {
  currentCompletePercentage?: number;
  requiredMillis?: number;
}

@Component({
  selector: 'app-fast-exploration-button',
  templateUrl: './fast-exploration-button.component.html',
  styleUrls: ['./fast-exploration-button.component.scss']
})
export class FastExplorationButtonComponent extends BaseComponent implements OnInit, OnDestroy {

  @Input() public planet: Planet;
  public runningExplorations: { [key: number]: DisplayQuadrantUnitRunningMission } = {};
  public hasFastExplorerUnits = false;
  public availableFastExploreUnits: ObtainedUnit[] = [];

  private _currentSourcePlanet: Planet;
  private _currentExplorationsProgressInterval: number;

  constructor(
    private _toastrService: ToastrService,
    private _missionService: MissionService,
    private _missionStore: MissionStore,
    private _missionInformationStore: MissionInformationStore,
  ) {
    super();
  }
  public ngOnInit(): void {
    this._subscriptions.add(this._missionInformationStore.originPlanet.subscribe(planet => this._currentSourcePlanet = planet));
    this._subscriptions.add(this._missionInformationStore.availableUnits.subscribe(obtainedUnits => {
      this.hasFastExplorerUnits = obtainedUnits.some(obtainedUnit => obtainedUnit.unit.canFastExplore);
      this.availableFastExploreUnits = this.hasFastExplorerUnits
        ? obtainedUnits.filter(obtainedUnit => obtainedUnit.unit.canFastExplore)
        : [];
    }));

    this._subscriptions.add(this._missionStore.myUnitMissions.subscribe(unitMissions => {
      this.runningExplorations = {};
      if (unitMissions.some(unitMission => unitMission.type === 'EXPLORE')) {
        unitMissions
          .filter(unitMission => unitMission.type === 'EXPLORE')
          .forEach(unitMission =>
            this.runningExplorations[unitMission.targetPlanet.id] = { ...unitMission, requiredMillis: unitMission.requiredTime * 1000 }
          );
        this._registerInterval();
      } else {
        this._clearExplorationProgressInterval();
      }
    }));
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   */
  public clickFastExplore(targetPlanet: Planet): void {
    if (this.hasFastExplorerUnits) {
      this._loadingService.runWithLoading(async () => {
        const ou: ObtainedUnit = this.availableFastExploreUnits[0];
        await this._missionService.sendExploreMission(this._currentSourcePlanet, targetPlanet, [
          {
            count: 1,
            unit: ou.unit,
            expirationId: ou?.temporalInformation?.id
          }
        ]).toPromise();
        this._toastrService.info('APP.DISPLAY_QUADRANT.FAST_EXPLORE_SENT', '', {
          unitName: ou.unit.name
        });
      });
    } else {
      this._toastrService.error('APP.DISPLAY_QUADRANT.FAST_EXPLORE_UNAVAILABLE');
    }
  }

  public ngOnDestroy(): void {
    super.ngOnDestroy();
    this._clearExplorationProgressInterval();
  }

  private _registerInterval(): void {
    this._currentExplorationsProgressInterval = window.setInterval(() => {
      Object.values(this.runningExplorations).forEach(unitRunningMission => {
        unitRunningMission.currentCompletePercentage = MissionUtil.computeProgressPercentage(unitRunningMission);
      });
    }, 400);
  }

  private _clearExplorationProgressInterval(): void {
    window.clearInterval(this._currentExplorationsProgressInterval);
    delete this._currentExplorationsProgressInterval;
  }

}
