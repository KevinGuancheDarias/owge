import { Component, OnInit, ViewEncapsulation, Input, ViewChild, OnDestroy } from '@angular/core';

import { BaseComponent } from '../../base/base.component';
import { NavigationData } from '../../shared/types/navigation-data.type';
import { MissionModalComponent } from '../../mission-modal/mission-modal.component';
import { NavigationConfig } from '../../shared/types/navigation-config.type';
import { NavigationService } from '../../service/navigation.service';
import { MissionInformationStore } from '../../store/mission-information.store';
import { PlanetService, PlanetListItem, PlanetListService, PlanetListAddEditModalComponent } from '@owge/galaxy';
import { Planet, TutorialService } from '@owge/universe';

@Component({
  selector: 'app-display-quadrant',
  templateUrl: './display-quadrant.component.html',
  styleUrls: ['./display-quadrant.component.less', './display-quadrant.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DisplayQuadrantComponent extends BaseComponent implements OnInit, OnDestroy {

  @Input() public isFullWidth = false;

  @ViewChild('missionModal', { static: true })
  private _missionModal: MissionModalComponent;

  @ViewChild('addEditModal')
  private _addEditModal: PlanetListAddEditModalComponent;

  public navigationData: NavigationData;
  public navigationConfig: NavigationConfig;
  public addingOrEditing: PlanetListItem;
  public planetList: { [key: number]: PlanetListItem } = {};


  constructor(
    private _navigationService: NavigationService,
    private _planetService: PlanetService,
    private _planetListService: PlanetListService,
    private _tutorialService: TutorialService,
    private _missionInformationStore: MissionInformationStore
  ) {
    super();
  }

  public async ngOnInit() {
    this.navigationConfig = await this._navigationService.findCurrentNavigationConfig();
    await this._navigationService.updateQueryParams(this.navigationConfig);
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
    this._subscriptions.add(
        this._planetListService.findAll().subscribe(list => {
          this.planetList = {};
          list.forEach(current => this.planetList[current.planet.id] = current);
        }),
        this._navigationService.findNavigationConfigFromUrl().subscribe(async navigationConfig => {
          this.navigationConfig = navigationConfig;
          this.navigationData = await this._navigationService.navigate(navigationConfig);
        })
    );
    await this._tutorialService.triggerTutorialAfterRender();
  }

  public async changePosition(newPosition: NavigationConfig): Promise<void> {
    await this._navigationService.updateQueryParams(newPosition);
  }

  /**
   * Returns the name of the selected galaxy
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
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
   */
  public addEdit(planet: Planet): void {
    const selected: PlanetListItem = this.planetList[planet.id];
    this.addingOrEditing = selected ? { ...selected } : { planet } as any;
    this._addEditModal.show();
  }

  public sendMission(targetPlanet: Planet) {
    this._missionInformationStore.targetPlanet.next(targetPlanet);
    this._missionModal.show();
    this._tutorialService.triggerTutorialAfterRender(600);
  }
}
