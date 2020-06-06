import { Component, OnInit, OnDestroy } from '@angular/core';

import { MissionInformationStore } from '../../store/mission-information.store';
import { UnitService } from '../../service/unit.service';
import { BaseComponent } from '../../base/base.component';
import { PlanetService } from '@owge/galaxy';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-navigation',
  templateUrl: './navigation.component.html',
  styleUrls: ['./navigation.component.less'],
  providers: [MissionInformationStore]
})
export class NavigationComponent extends BaseComponent implements OnInit, OnDestroy {

  private _findInMyPlanetSubscription: Subscription;

  constructor(
    private _missioninformationStore: MissionInformationStore,
    private _unitService: UnitService,
    private _planetService: PlanetService
  ) {
    super();
  }

  public ngOnInit(): void {
    this._subscriptions.add(this._planetService.findCurrentPlanet().subscribe(selectedPlanet => {
      this._missioninformationStore.originPlanet.next(selectedPlanet);
      this._clearFindInMyPlanets();
      this._findInMyPlanetSubscription = this._unitService.findInMyPlanet(selectedPlanet.id).subscribe(units => {
        this._missioninformationStore.availableUnits.next(units);
      });
    }));
  }

  public ngOnDestroy(): void {
    this._clearFindInMyPlanets();
    super.ngOnDestroy();
  }

  private _clearFindInMyPlanets(): void {
    if (this._findInMyPlanetSubscription) {
      this._findInMyPlanetSubscription.unsubscribe();
      delete this._findInMyPlanetSubscription;
    }
  }
}
