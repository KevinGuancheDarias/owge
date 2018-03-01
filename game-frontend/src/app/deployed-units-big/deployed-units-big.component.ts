import { ObtainedUnit } from '../shared-pojo/obtained-unit.pojo';
import { BaseComponent } from '../base/base.component';
import { UnitPojo } from '../shared-pojo/unit.pojo';
import { Component, OnInit } from '@angular/core';
import { UnitService } from 'app/service/unit.service';

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
    this.loginSessionService.findSelectedPlanet.filter(planet => !!planet).subscribe(planet => {
      this._unitService.findInMyPlanet(planet.id).subscribe(units => this.obtainedUnits = units);
    });
  }

}
