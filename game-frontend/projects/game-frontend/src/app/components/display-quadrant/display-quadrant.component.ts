import { Component, OnInit, ViewEncapsulation, Input, ViewChild, OnDestroy } from '@angular/core';

import { BaseComponent } from '../../base/base.component';
import { NavigationData } from '../../shared/types/navigation-data.type';
import { MissionModalComponent } from '../../mission-modal/mission-modal.component';
import { NavigationConfig } from '../../shared/types/navigation-config.type';
import { NavigationService } from '../../service/navigation.service';
import { MissionInformationStore } from '../../store/mission-information.store';
import { PlanetService, PlanetListItem, PlanetListService, PlanetListAddEditModalComponent } from '@owge/galaxy';
import { Planet, ObtainedUnit, Unit, MissionStore, UnitRunningMission } from '@owge/universe';
import { MissionService } from '../../services/mission.service';
import { ToastrService } from '@owge/core';
import { TutorialService } from 'projects/owge-universe/src/lib/services/tutorial.service';

interface DisplayQuadrantUnitRunningMission extends UnitRunningMission {
  currentCompletePercentage?: number;
  requiredMillis?: number;
}

@Component({
  selector: 'app-display-quadrant',
  templateUrl: './display-quadrant.component.html',
  styleUrls: ['./display-quadrant.component.less', './display-quadrant.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DisplayQuadrantComponent extends BaseComponent implements OnInit, OnDestroy {

  @Input() public isFullWidth = false;

  public navigationData: NavigationData;
  public navigationConfig: NavigationConfig;
  public addingOrEditing: PlanetListItem;
  public planetList: { [key: number]: PlanetListItem } = {};
  public hasFastExplorerUnits = false;
  public availableFastExploreUnits: ObtainedUnit[] = [];
  public runningExplorations: { [key: number]: DisplayQuadrantUnitRunningMission } = {};

  @ViewChild('missionModal', { static: true })
  private _missionModal: MissionModalComponent;

  @ViewChild('addEditModal')
  private _addEditModal: PlanetListAddEditModalComponent;

  private _currentSourcePlanet: Planet;
  private _currentExplorationsProgressInterval: number;
  constructor(
    private _navigationService: NavigationService,
    private _missionInformationStore: MissionInformationStore,
    private _planetService: PlanetService,
    private _planetListService: PlanetListService,
    private _missionService: MissionService,
    private _toastrService: ToastrService,
    private _missionStore: MissionStore,
    private _tutorialService: TutorialService
  ) {
    super();
  }

  public async ngOnInit() {
    this.navigationConfig = await this._navigationService.findCurrentNavigationConfig();
    this.navigationData = await this._navigationService.navigate(this.navigationConfig);
    this._subscriptions.add(this._planetService.onPlanetExplored().subscribe(async explored => {
      if (explored) {
        const planetIndex: number = this.navigationData.planets
          .findIndex(current => !current.richness && current.id === explored.id);
        if (planetIndex !== -1) {
          this.navigationData.planets[planetIndex] = explored;
        }
      } else {
        this.navigationData = await this._doWithLoading(this._navigationService.navigate(this.navigationConfig));
      }
    }));
    this._subscriptions.add(this._missionInformationStore.originPlanet.subscribe(planet => this._currentSourcePlanet = planet));
    this._subscriptions.add(this._missionInformationStore.availableUnits.subscribe(obtainedUnits => {
      this.hasFastExplorerUnits = obtainedUnits.some(obtainedUnit => obtainedUnit.unit.canFastExplore);
      this.availableFastExploreUnits = this.hasFastExplorerUnits
        ? obtainedUnits.filter(obtainedUnit => obtainedUnit.unit.canFastExplore)
        : [];
    }));
    this._subscriptions.add(this._planetListService.findAll()
      .subscribe(list => {
        this.planetList = {};
        list.forEach(current => this.planetList[current.planet.id] = current);
      })
    );
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
    this._tutorialService.triggerTutorialAfterRender();
  }

  public ngOnDestroy(): void {
    super.ngOnDestroy();
    this._clearExplorationProgressInterval();
  }

  public async changePosition(newPosition: NavigationConfig): Promise<void> {
    this.navigationConfig = newPosition;
    this.navigationData = await this._navigationService.navigate(newPosition);
  }

  /**
   * Returns the name of the selected galaxy
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @returns {string}
   * @memberof NavigationComponent
   */
  public findSelectedGalaxyName(): string {
    return this.navigationData.galaxies.find(current => current.id === this.navigationConfig.galaxy).name;
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param [selected]
   */
  public addEdit(planet: Planet): void {
    const selected: PlanetListItem = this.planetList[planet.id];
    this.addingOrEditing = selected ? { ...selected } : <any>{ planet };
    this._addEditModal.show();
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
        const unit: Unit = this.availableFastExploreUnits[0].unit;
        await this._missionService.sendExploreMission(this._currentSourcePlanet, targetPlanet, [
          {
            count: 1,
            unit
          }
        ]).toPromise();
        this._toastrService.info('APP.DISPLAY_QUADRANT.FAST_EXPLORE_SENT', '', {
          unitName: unit.name
        });
      });
    } else {
      this._toastrService.error('APP.DISPLAY_QUADRANT.FAST_EXPLORE_UNAVAILABLE');
    }
  }

  public sendMission(targetPlanet: Planet) {
    this._missionInformationStore.targetPlanet.next(targetPlanet);
    this._missionModal.show();
    this._tutorialService.triggerTutorialAfterRender(600);
  }

  private _registerInterval(): void {
    this._currentExplorationsProgressInterval = window.setInterval(() => {
      Object.values(this.runningExplorations).forEach(unitRunningMission => {
        const currentPendingMillis = unitRunningMission.browserComputedTerminationDate.getTime() - new Date().getTime();
        let percentage = Math.floor((currentPendingMillis / unitRunningMission.requiredMillis) * 100);
        percentage = percentage > 100 ? 100 : percentage;
        unitRunningMission.currentCompletePercentage = Math.abs(percentage - 100);
      });
    }, 400);
  }

  private _clearExplorationProgressInterval(): void {
    window.clearInterval(this._currentExplorationsProgressInterval);
    delete this._currentExplorationsProgressInterval;
  }
}
