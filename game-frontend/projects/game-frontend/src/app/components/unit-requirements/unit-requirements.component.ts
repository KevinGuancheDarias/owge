import { Component, OnInit } from '@angular/core';

import { UnitService } from '../../service/unit.service';
import { BaseUnitComponent } from '../../shared/base-unit.component';
import { UnitUpgradeRequirements } from '@owge/universe';

@Component({
  selector: 'app-unit-requirements',
  templateUrl: './unit-requirements.component.html',
  styleUrls: ['./unit-requirements.component.scss']
})
export class UnitRequirementsComponent extends BaseUnitComponent implements OnInit {
  public unitRequirements: UnitUpgradeRequirements[];

  constructor(private _unitService: UnitService) {
    super();
  }

  ngOnInit(): void {
    this._subscriptions.add(this._unitService.findUnitUpgradeRequirements().subscribe(result => this.unitRequirements = result));
  }
}
