import { Component, OnInit, ViewEncapsulation } from '@angular/core';

import { UnitService } from '../../service/unit.service';
import { UnitUpgradeRequirements } from '../../shared/types/unit-upgrade-requirements.type';
import { BaseUnitComponent } from '../../shared/base-unit.component';

interface WithAllReachedUnitUpgradeRequirements extends UnitUpgradeRequirements {
  allReached?: boolean;
}

@Component({
  selector: 'app-unit-requirements',
  templateUrl: './unit-requirements.component.html',
  styleUrls: ['./unit-requirements.component.scss']
})
export class UnitRequirementsComponent extends BaseUnitComponent implements OnInit {
  public unitRequirements: WithAllReachedUnitUpgradeRequirements[];

  constructor(private _unitService: UnitService) {
    super();
  }

  async ngOnInit(): Promise<void> {
    this.unitRequirements = await this._unitService.findUnitUpgradeRequirements().toPromise();
    this.unitRequirements.forEach(current => {
      current.allReached = current.requirements.every(requirement => requirement.reached);
    });
  }
}
