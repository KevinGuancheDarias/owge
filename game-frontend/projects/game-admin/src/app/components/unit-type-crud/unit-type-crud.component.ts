import { Component, OnInit, ViewChild } from '@angular/core';
import { AttackRule, MissionSupport, SpeedImpactGroup, UnitType } from '@owge/core';
import { take } from 'rxjs/operators';
import { AdminSpeedImpactGroupService } from '../../services/admin-speed-impact-group.service';
import { AdminUnitTypeService } from '../../services/admin-unit-type.service';
import { RulesModalComponent } from '../rules-modal/rules-modal.component';



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
export class UnitTypeCrudComponent implements OnInit {
  private static readonly _DEFAULT_CAN_DO_MISSION: MissionSupport = 'ANY';

  @ViewChild(RulesModalComponent) rulesModal: RulesModalComponent;

  unitTypes: UnitType[];
  unitTypesWithLimitedCount: UnitType[] = [];
  unitTypesForParentSelect: UnitType[] = [];
  unitType: UnitType & { attackRule: AttackRule };
  isUnlimitedMaxAmount: boolean;
  speedImpactGroups: SpeedImpactGroup[] = [];
  beforeAttackRuleDeleteBinded: () => Promise<void>;
  beforeCriticalAttackDeleteBinded: () => Promise<void>;

  constructor(public adminUnitTypeService: AdminUnitTypeService, private _adminSpeedImpactGroupService: AdminSpeedImpactGroupService) { }

  ngOnInit(): void {
    this.beforeAttackRuleDeleteBinded = this.beforeAttackRuleDelete.bind(this);
    this.beforeCriticalAttackDeleteBinded = this.beforeCriticalAttackDelete.bind(this);
    this.adminUnitTypeService.findAll().subscribe(result => {
      this.unitTypes = result;
      this._computeAvailableTypesForSelects();
    });
    this._adminSpeedImpactGroupService.findAll().subscribe(result => this.speedImpactGroups = result);
  }

  onIsUnlimitedMaxAmountChange(): void {
    this.unitType.maxCount = null;
  }

  onSelectedOrNew(el: UnitType & { attackRule: AttackRule }): void {
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
    this._computeAvailableTypesForSelects();
    this.isUnlimitedMaxAmount = typeof this.unitType.maxCount !== 'number';
  }

  isSameObject(a: SpeedImpactGroup, b: SpeedImpactGroup): boolean {
    return a === b || (a && b && a.id === b.id);
  }

  async beforeAttackRuleDelete(): Promise<void> {
    return this.adminUnitTypeService.unsetAttackRule(this.unitType).pipe(take(1)).toPromise();
  }

  async beforeCriticalAttackDelete(): Promise<void> {
    await this.adminUnitTypeService.unsetCriticalAttack(this.unitType).pipe(take(1)).toPromise();
    delete this.unitType.criticalAttack;
  }

  private _computeAvailableTypesForSelects() {
    const hasUnitType: boolean = this.unitType && !!this.unitType.id;
    this.unitTypesWithLimitedCount = this.unitTypes.filter(current => current.maxCount);
    if (hasUnitType) {
      this.unitTypesWithLimitedCount = this.unitTypesWithLimitedCount.filter(current => current.id !== this.unitType.id);
    }

    this.unitTypesForParentSelect = hasUnitType
      ? this.unitTypes.filter(current => current.id !== this.unitType.id)
      : this.unitTypes;
  }
}
