import { Component } from '@angular/core';

import { UnitType, MissionSupport } from '@owge/universe';

import { AdminUnitTypeService } from '../../services/admin-unit-type.service';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 * @class UnitTypeCrudComponent
 */
@Component({
  selector: 'app-unit-type-crud',
  templateUrl: './unit-type-crud.component.html',
  styleUrls: ['./unit-type-crud.component.scss']
})
export class UnitTypeCrudComponent {
  private static readonly _DEFAULT_CAN_DO_MISSION: MissionSupport = 'ANY';

  public unitType: UnitType;
  public isUnlimitedMaxAmount: boolean;
  public validCanMissionOptions: MissionSupport[] = [
    'NONE',
    'OWNED_ONLY',
    'ANY'
  ];

  constructor(public adminUnitTypeService: AdminUnitTypeService) { }

  public onIsUnlimitedMaxAmountChange(): void {
    this.unitType.maxCount = null;
  }

  public onSelectedOrNew(el: UnitType): void {
    this.unitType = el;
    if (!el.id) {
      el.canExplore = UnitTypeCrudComponent._DEFAULT_CAN_DO_MISSION;
      el.canGather = UnitTypeCrudComponent._DEFAULT_CAN_DO_MISSION;
      el.canEstablishBase = UnitTypeCrudComponent._DEFAULT_CAN_DO_MISSION;
      el.canAttack = UnitTypeCrudComponent._DEFAULT_CAN_DO_MISSION;
      el.canCounterattack = UnitTypeCrudComponent._DEFAULT_CAN_DO_MISSION;
      el.canConquest = UnitTypeCrudComponent._DEFAULT_CAN_DO_MISSION;
      el.canDeploy = UnitTypeCrudComponent._DEFAULT_CAN_DO_MISSION;
    }
    this.isUnlimitedMaxAmount = typeof this.unitType.maxCount !== 'number';
  }
}
