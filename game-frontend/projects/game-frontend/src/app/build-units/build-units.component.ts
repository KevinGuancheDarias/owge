import { Component, OnInit, Input } from '@angular/core';
import {filter} from 'rxjs/operators';

import { PlanetStore } from '@owge/galaxy';

import { UnitService, RunningUnitIntervalInformation } from './../service/unit.service';
import { UnitPojo } from './../shared-pojo/unit.pojo'; import { BaseUnitComponent } from '../shared/base-unit.component';

@Component({
  selector: 'app-build-units',
  templateUrl: './build-units.component.html',
  styleUrls: ['./build-units.component.less']
})
export class BuildUnitsComponent extends BaseUnitComponent implements OnInit {

  public get unlockedUnits(): UnitPojo {
    return this._unlockedUnits;
  }
  private _unlockedUnits: UnitPojo;

  public buildingUnit: RunningUnitIntervalInformation;

  constructor(private _unitService: UnitService, private _planetStore: PlanetStore) {
    super();
  }

  public ngOnInit() {
    this.requireUser();
    this.findUnlocked();

    this._planetStore.selectedPlanet.pipe(filter(planet => !!planet)).subscribe(() => {
      this._unitService.planetsLoaded.pipe(filter(value => !!value))
        .subscribe(() => this.buildingUnit = this._unitService.findIsRunningInSelectedPlanet());
    });
  }

  private findUnlocked(): void {
    this._unitService.findUnlocked().subscribe(unlockedUnits => this._unlockedUnits = unlockedUnits);
  }
}
