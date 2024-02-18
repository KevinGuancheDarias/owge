import { Component, OnInit, OnDestroy } from '@angular/core';

import { PlanetService } from '@owge/galaxy';

import { UnitService } from '../service/unit.service';
import { BaseUnitComponent } from '../shared/base-unit.component';
import { Subscription } from 'rxjs';
import { cloneDeep } from 'lodash-es';
import { ObtainedUnit } from '@owge/types/universe';

@Component({
  selector: 'app-deployed-units-big',
  templateUrl: './deployed-units-big.component.html',
  styleUrls: ['./deployed-units-big.component.scss']
})
export class DeployedUnitsBigComponent extends BaseUnitComponent implements OnInit, OnDestroy {

  public obtainedUnits: ObtainedUnit[];

  private _findInMyPlanetSubscription: Subscription;

  constructor(private _unitService: UnitService, private _planetService: PlanetService) {
    super();
  }

  public ngOnInit() {
    this._findInMyPlanet();
  }

  public ngOnDestroy(): void {
    super.ngOnDestroy();
    this._clearFindInMyPlanetSubscription();
  }

  private _findInMyPlanet(): void {
    this._subscriptions.add(this._planetService.findCurrentPlanet().subscribe(planet => {
      this._clearFindInMyPlanetSubscription();
      this._findInMyPlanetSubscription = this._unitService.findInMyPlanet(planet.id)
        .subscribe(units => this.obtainedUnits = cloneDeep(units));
    }));
  }

  private _clearFindInMyPlanetSubscription(): void {
    if (this._findInMyPlanetSubscription) {
      this._findInMyPlanetSubscription.unsubscribe();
      delete this._findInMyPlanetSubscription;
    }
  }

}
