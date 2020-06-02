import { Component, OnInit, ViewEncapsulation, Input, ViewChild, ChangeDetectorRef } from '@angular/core';

import { BaseComponent } from '../../base/base.component';
import { NavigationData } from '../../shared/types/navigation-data.type';
import { PlanetPojo } from '../../shared-pojo/planet.pojo';
import { MissionModalComponent } from '../../mission-modal/mission-modal.component';
import { NavigationConfig } from '../../shared/types/navigation-config.type';
import { NavigationService } from '../../service/navigation.service';
import { MissionInformationStore } from '../../store/mission-information.store';
import { PlanetService, Planet } from '@owge/galaxy';

@Component({
  selector: 'app-display-quadrant',
  templateUrl: './display-quadrant.component.html',
  styleUrls: ['./display-quadrant.component.less'],
  encapsulation: ViewEncapsulation.None
})
export class DisplayQuadrantComponent extends BaseComponent implements OnInit {

  @Input() public isFullWidth = false;

  public navigationData: NavigationData;
  public navigationConfig: NavigationConfig;

  @ViewChild('missionModal', { static: true })
  private _missionModal: MissionModalComponent;

  constructor(
    private _navigationService: NavigationService,
    private _missionInformationStore: MissionInformationStore,
    private _planetService: PlanetService
  ) {
    super();
  }

  public async ngOnInit() {
    this.navigationConfig = await this._navigationService.findCurrentNavigationConfig();
    this.navigationData = await this._navigationService.navigate(this.navigationConfig);
    this._subscriptions.add(this._planetService.onPlanetExplored().subscribe(explored => {
      const exploredPlanedWithoutProps: Planet = this.navigationData.planets
        .find(current => !current.richness && current.id === explored.id);
      if (exploredPlanedWithoutProps) {
        Object.assign(exploredPlanedWithoutProps, explored);
      }
    }));
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

  public sendMission(targetPlanet: PlanetPojo) {
    this._missionInformationStore.targetPlanet.next(targetPlanet);
    this._missionModal.show();
  }
}
