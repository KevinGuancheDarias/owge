import { Component, OnInit } from '@angular/core';

import { ObtainedUnit } from '../shared-pojo/obtained-unit.pojo';
import { BaseComponent } from '../base/base.component';
import { UnitService } from '../service/unit.service';

@Component({
  selector: 'app-deployed-units-big',
  templateUrl: './deployed-units-big.component.html',
  styleUrls: ['./deployed-units-big.component.less']
})
export class DeployedUnitsBigComponent extends BaseComponent implements OnInit {

  public obtainedUnits: ObtainedUnit[];

  constructor(private _unitService: UnitService) {
    super();
  }

  public ngOnInit() {
    this._findInMyPlanet();
  }

  public onDeletion(): void {
    delete this.obtainedUnits;
    this._findInMyPlanet();
  }

  private _findInMyPlanet(): void {
    this.loginSessionService.findSelectedPlanet.filter(planet => !!planet).subscribe(planet => {
      this._unitService.findInMyPlanet(planet.id).subscribe(units => this.obtainedUnits = units);
    });
  }

}
