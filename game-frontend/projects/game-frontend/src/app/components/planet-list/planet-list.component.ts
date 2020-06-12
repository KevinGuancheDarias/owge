import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { Planet } from '@owge/universe';
import { BaseComponent } from '../../base/base.component';
import { MissionModalComponent } from '../../mission-modal/mission-modal.component';
import { MissionInformationStore } from '../../store/mission-information.store';
import { PlanetService, PlanetListItem, PlanetListService, PlanetListAddEditModalComponent, } from '@owge/galaxy';
import { Subscription } from 'rxjs';
import { UnitService } from '../../service/unit.service';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-planet-list',
  templateUrl: './planet-list.component.html',
  styleUrls: ['./planet-list.component.scss'],
  providers: [MissionInformationStore]
})
export class PlanetListComponent extends BaseComponent implements OnInit, OnDestroy {

  public addingOrEditing: PlanetListItem;
  public wantOwnedOnly = false;
  public list: PlanetListItem[];
  public filteredList: PlanetListItem[];

  @ViewChild('addEditModal')
  private _addEditModal: PlanetListAddEditModalComponent;

  @ViewChild('missionModal', { static: true })
  private _missionModal: MissionModalComponent;

  private _findInMyPlanetSubscription: Subscription;

  constructor(
    private _planetService: PlanetService,
    private _unitService: UnitService,
    private _missionInformationStore: MissionInformationStore,
    planetListService: PlanetListService
  ) {
    super();
    this._subscriptions.add(planetListService.findAll().subscribe(content => {
      this.list = content;
      this._applyFilters();
    }));
  }

  public ngOnInit(): void {
    this._subscriptions.add(this._planetService.findCurrentPlanet().subscribe(selectedPlanet => {
      this._missionInformationStore.originPlanet.next(selectedPlanet);
      this._clearFindInMyPlanets();
      this._findInMyPlanetSubscription = this._unitService.findInMyPlanet(selectedPlanet.id).subscribe(units => {
        this._missionInformationStore.availableUnits.next(units);
      });
    }));
  }

  public ngOnDestroy(): void {
    super.ngOnDestroy();
    this._clearFindInMyPlanets();
  }

  public addEdit(el: PlanetListItem): void {
    this.addingOrEditing = { ...el };
    this._addEditModal.show();
  }

  public sendMission(targetPlanet: Planet) {
    this._missionInformationStore.targetPlanet.next(targetPlanet);
    this._missionModal.show();
  }

  public onChange(): void {
    this._applyFilters();
  }

  private _clearFindInMyPlanets(): void {
    if (this._findInMyPlanetSubscription) {
      this._findInMyPlanetSubscription.unsubscribe();
      delete this._findInMyPlanetSubscription;
    }
  }

  private _applyFilters(): void {
    this.filteredList = this.wantOwnedOnly ? this.list.filter(current => current.planet.ownerId) : this.list;
  }
}
