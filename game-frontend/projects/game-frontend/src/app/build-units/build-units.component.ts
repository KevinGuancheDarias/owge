import { Component, OnDestroy, OnInit } from '@angular/core';
import { PlanetService } from '@owge/galaxy';
import { Unit, UnitBuildRunningMission } from '@owge/types/universe';
import { Observable, Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import { BaseUnitComponent } from '../shared/base-unit.component';
import { UnitService } from './../service/unit.service';


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
    this._subscriptions.add(this._findUnlocked().subscribe(unlockedUnits => this._unlockedUnits = unlockedUnits));

    this._subscriptions.add(this._planetService.findCurrentPlanet().subscribe(planet => {
      if (this._buildingSubscription) {
        this._buildingSubscription.unsubscribe();
        delete this._buildingSubscription;
      }
      this._buildingSubscription = this._unitService.findBuildingMissionInMyPlanet(planet.id).subscribe(async buildingMission => {
        this.building = buildingMission;
        const units = [...await this._findUnlocked().pipe(take(1)).toPromise()];
        if (buildingMission && !units.some(unit => unit.id === buildingMission.unit.id)) {
          units.push(buildingMission.unit);
        }
        this._unlockedUnits = units;
      });
    }));
  }

  public ngOnDestroy(): void {
    super.ngOnDestroy();
    if (this._buildingSubscription) {
      this._buildingSubscription.unsubscribe();
    }
  }

  private _findUnlocked(): Observable<Unit[]> {
    return this._unitService.findUnlocked();
  }
}
