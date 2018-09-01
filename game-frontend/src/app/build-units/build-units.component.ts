import { UnitService, RunningUnitIntervalInformation } from './../service/unit.service';
import { UnitPojo } from './../shared-pojo/unit.pojo';
import { Component, OnInit, Input } from '@angular/core';
import { BaseUnitComponent } from '../shared/base-unit.component';

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

  constructor(private _unitService: UnitService) {
    super();
  }

  public ngOnInit() {
    this.requireUser();
    this.findUnlocked();

    this.loginSessionService.findSelectedPlanet.filter(planet => !!planet).subscribe(() => {
      this._unitService.planetsLoaded.filter(value => !!value)
        .subscribe(() => this.buildingUnit = this._unitService.findIsRunningInSelectedPlanet());
    });
  }

  private findUnlocked(): void {
    this._unitService.findUnlocked().subscribe(unlockedUnits => this._unlockedUnits = unlockedUnits);
  }
}
