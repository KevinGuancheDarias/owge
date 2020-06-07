import { Component, OnInit, Input, OnDestroy } from '@angular/core';
import { filter } from 'rxjs/operators';

import { PlanetService } from '@owge/galaxy';

import { UnitService } from './../service/unit.service';
import { BaseUnitComponent } from '../shared/base-unit.component';
import { Unit, UnitBuildRunningMission } from '@owge/universe';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-build-units',
  templateUrl: './build-units.component.html',
  styleUrls: ['./build-units.component.scss']
})
export class BuildUnitsComponent extends BaseUnitComponent implements OnInit, OnDestroy {
  public building: UnitBuildRunningMission;

  public get unlockedUnits(): Unit[] {
    return this._unlockedUnits;
  }
  private _unlockedUnits: Unit[];
  private _buildingSubscription: Subscription;

  constructor(private _unitService: UnitService, private _planetService: PlanetService) {
    super();
  }

  public ngOnInit() {
    this.requireUser();
    this.findUnlocked();

    this._subscriptions.add(this._planetService.findCurrentPlanet().subscribe(planet => {
      if (this._buildingSubscription) {
        this._buildingSubscription.unsubscribe();
        delete this._buildingSubscription;
      }
      this._buildingSubscription = this._unitService.findBuildingMissionInMyPlanet(planet.id).subscribe(
        buildingMission => this.building = buildingMission
      );
    }));
  }

  public ngOnDestroy(): void {
    super.ngOnDestroy();
    if (this._buildingSubscription) {
      this._buildingSubscription.unsubscribe();
    }
  }

  private findUnlocked(): void {
    this._subscriptions.add(this._unitService.findUnlocked().subscribe(unlockedUnits => this._unlockedUnits = unlockedUnits));
  }
}
