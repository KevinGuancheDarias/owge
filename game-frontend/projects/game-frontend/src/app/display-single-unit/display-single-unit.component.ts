import {
  Component,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {ModalComponent} from '@owge/core';
import { UnitType, User } from '@owge/types/core';
import { UserWithFaction } from '@owge/types/faction';
import {
  ImprovementUtil, UserStorage, UnitTypeService, UnitRuleFinderService, ActiveTimeSpecialRuleFinderService
} from '@owge/universe';
import { ObtainedUnit, Unit, UnitBuildRunningMission, Rule, RuleWithUnitEntity } from '@owge/types/universe';
import { WidgetConfirmationDialogComponent } from '@owge/widgets';
import { BehaviorSubject, Subscription } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { CriticalAttackInformation } from '../types/critical-attack-information.type';
import { BaseComponent } from '../base/base.component';
import { UnitService } from '../service/unit.service';
import {RuleService} from '../../../../../modules/owge-universe/src/lib/services/rule.service';

export type ValidViews = 'requirements' | 'attributes' | 'improvements';

export interface AttackableUnitType extends UnitType {
  isAttackable?: boolean;
}

@Component({
  selector: 'app-display-single-unit',
  templateUrl: './display-single-unit.component.html',
  styleUrls: ['./display-single-unit.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DisplaySingleUnitComponent extends BaseComponent<UserWithFaction> implements OnInit, OnChanges, OnDestroy {

  @Input()
  public unit: Unit;

  @Input() isCompactView = false;

  @Input()
  public building: UnitBuildRunningMission;

  @Input()
  public withBuildMode = false;

  @Input()
  public withInPlanetMode = false;

  /**
   * If specified, allows deleting the specified obtained unit
   *
   * @memberof DisplaySingleUnitComponent
   */
  @Input()
  public isDeletable = false;

  /**
   * In planet count
   *
   * @type {number}
   * @memberof DisplaySingleUnitComponent
   */
  @Input()
  public inPlanetCount: number;

  @Input()
  public obtainedUnit: ObtainedUnit;

  /**
   * What to display by default?, requirements?, or attributes? <br>
   * Defaults to <b>attributes</b>
   *
   * @type {ValidViews}
   * @memberof DisplaySingleUnitComponent
   */
  @Input()
  public defaultView: ValidViews = 'attributes';

  @ViewChild(WidgetConfirmationDialogComponent) public confirmDialog: WidgetConfirmationDialogComponent;
  @ViewChild(ModalComponent) public modal: ModalComponent;

  selectedView: ValidViews;
  numberToDelete: number;
  moreAttack: number;
  moreShield: number;
  moreHealth: number;
  moreCharge: number;
  moreSpeed: number;
  unitTypes: UnitType[];
  unitType: UnitType;
  attackableUnitTypes: AttackableUnitType[];
  criticalAttackInformations: CriticalAttackInformation[];
  isDefaultCriticalDisplayed = false;
  rulesForCapturingUnits: RuleWithUnitEntity[];
  rulesForCapturingUnitsTimeSpecial: RuleWithUnitEntity[];
  rulesForInvisibleUnitsTimeSpecial: RuleWithUnitEntity[];

  get count(): any {
    return this._count.value;
  }

  set count(value: any) {
    let targetValue;
    if (isNaN(parseInt(value, 10))) {
      targetValue = '';
    } else if (value < 1) {
      targetValue = 1;
    } else {
      targetValue = value;
    }
    if (targetValue !== this.count) {
      this._count.next(targetValue);
    }
  }
  private _count: BehaviorSubject<number> = new BehaviorSubject(1);

  private _improvementsSubscription: Subscription;
  private loadAffectingRulesSubscription: Subscription;
  private loadAffectingActiveTimeSpecialRulesSubscription: Subscription;
  private loadAffectingActiveTimeSpecialHiddenUnitSubscription: Subscription;
  private unitTypeOfUnit: UnitType;
  private resourcesSubscription: Subscription;

  constructor(
    private _unitService: UnitService,
    private _unitTypeService: UnitTypeService,
    private _userStore: UserStorage<User>,
    private unitRuleFinderService: UnitRuleFinderService,
    private ruleService: RuleService,
    private activeTimeSpecialRuleFinderService: ActiveTimeSpecialRuleFinderService
  ) {
    super();
  }

  ngOnInit() {
    this.requireUser(() => {
      if (this._improvementsSubscription) {
        this._improvementsSubscription.unsubscribe();
        delete this._improvementsSubscription;
      }
      this._improvementsSubscription = this._userStore.currentUserImprovements.subscribe(async improvement => {
        this.moreCharge = improvement.moreChargeCapacity;
        await this.loadUnitTypeOfUnit();
        if (this.unitTypeOfUnit) {
          this.moreAttack = ImprovementUtil.findUnitTypeImprovement(improvement, 'ATTACK', this.unitTypeOfUnit);
          this.moreShield = ImprovementUtil.findUnitTypeImprovement(improvement, 'SHIELD', this.unitTypeOfUnit);
          this.moreHealth = ImprovementUtil.findUnitTypeImprovement(improvement, 'DEFENSE', this.unitTypeOfUnit);
          this.moreSpeed = ImprovementUtil.findUnitTypeImprovement(improvement, 'SPEED', this.unitTypeOfUnit);
        } else {
          console.warn(`Unit with id ${this.unit.id} doesn't have a unitType`);
        }
        if(this.unit) {
          this.handleUnitLoad(this.unit);
        }
        this._subscriptions.add(this._unitTypeService.getUnitTypes().subscribe(val => this.unitTypes = val));
        this.unit = this._unitService.computeRequiredResources(
            this.unit,
            true,
            this._count,
            (resourcesSubscription, improvementSubscription) => {
              this.resourcesSubscription = resourcesSubscription;
              if(improvementSubscription) {
                this._subscriptions.add(improvementSubscription);
              }
            }
        );
      });
    });
    this.selectedView = this.defaultView;
  }

  async ngOnChanges(changes: SimpleChanges): Promise<void> {
    const change = changes.unit;
    if(change) {
      const previous: Unit = change.previousValue;
      const currentValue: Unit = change.currentValue;
      if (previous?.id && previous?.id !== currentValue?.id) {
        this.unit = currentValue;
        await this.loadUnitTypeOfUnit();
        this.handleUnitLoad(currentValue);
      }
    }
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.loadAffectingActiveTimeSpecialRulesSubscription?.unsubscribe();
    this.loadAffectingRulesSubscription?.unsubscribe();
    this.loadAffectingActiveTimeSpecialHiddenUnitSubscription?.unsubscribe();
    this.resourcesSubscription?.unsubscribe();
    this._improvementsSubscription?.unsubscribe();
  }

  otherUnitAlreadyRunning(): void {
    this.displayError('Ya hay otras unidades en construcción');
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.20
   */
  clickOpenUnitInfo(): void {
    const unitUnitType: UnitType = this.unitTypes.find(current => current.id === this.unit.typeId);
    this.attackableUnitTypes = this.unitTypes.filter(unitType => unitType.used);
    this.attackableUnitTypes.forEach(
      unitType => unitType.isAttackable = this._unitTypeService.canAttack(
        this._unitTypeService.findAppliedAttackRule(unitUnitType),
        unitType
      )
    );
    this.modal.show();
    this._loadingService.addPromise(
      this._unitService.findCriticalAttackInformation(this.unit).toPromise()
    ).then(result => this.criticalAttackInformations = result);
  }

  cancelBuild(confirm: boolean): void {
    if (confirm) {
      this._unitService.cancel(this.building).pipe(take(1)).subscribe();
    }
  }

  async deleteUnits(): Promise<void> {
    if (await this.displayConfirm('Are you sure you want to delete the unit?')) {
      await this._doWithLoading(this._unitService.deleteObtainedUnit(this.obtainedUnit, this.numberToDelete));
    }
  }

  buildSelectedUnit(): void {
    this._unitService.registerUnitBuild(this.unit, this.count);
  }

  noResources(): void {
    this.displayError('No se poseen los recursos necesarios');
  }

  isValidDeletion(): boolean {
    return this.numberToDelete && this.numberToDelete <= this.inPlanetCount;
  }

  canBuild(): boolean {
    return this.unit.requirements.runnable && this._unitTypeService.hasAvailable(this.unit.typeId, this.count);
  }

  /**
   * If unit is unique should not allow more than one as <i>count</i> value
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @returns
   * @memberof DisplaySingleUnitComponent
   */
  isValidCount(): boolean {
    return (this.unit.isUnique && this.count === 1) || !this.unit.isUnique;
  }

  private handleUnitLoad(currentValue: Unit) {
    this.loadAffectingRulesSubscription?.unsubscribe();
    this.loadAffectingActiveTimeSpecialRulesSubscription?.unsubscribe();
    this.loadAffectingActiveTimeSpecialHiddenUnitSubscription?.unsubscribe();
    this.loadAffectingRulesSubscription = this.unitRuleFinderService.findRulesForUnit('UNIT_CAPTURE', currentValue)
        .subscribe(async rules => this.rulesForCapturingUnits = await this.ruleService.addRelatedUnits(rules));
    this.loadAffectingActiveTimeSpecialRulesSubscription = this.activeTimeSpecialRuleFinderService
        .findActiveRules('UNIT_CAPTURE')
        .subscribe(async rules => this.rulesForCapturingUnitsTimeSpecial = await this.ruleService.addRelatedUnits(rules));
    this.loadAffectingActiveTimeSpecialHiddenUnitSubscription = this.activeTimeSpecialRuleFinderService
        .findActiveRules('TIME_SPECIAL_IS_ENABLED_DO_HIDE', rule => this.isForUnit(rule, currentValue))
        .subscribe(async rules => this.rulesForInvisibleUnitsTimeSpecial = await this.ruleService.addRelatedUnits(rules));
  }

  private isForUnit(rule: Rule, unit: Unit): boolean {
    return (rule.destinationType === 'UNIT' && rule.destinationId === unit.id)
      || (rule.destinationType === 'UNIT_TYPE'
            && this._unitTypeService.isSameUnitTypeOrChild(rule.destinationId, this.unitTypeOfUnit, this.unitTypes)
        );
  }

  private async loadUnitTypeOfUnit() {
    const unitTypes: UnitType[] = await this._unitTypeService.getUnitTypes().pipe(
        filter(result => !!result),
        take(1)
    ).toPromise();
    this.unitTypeOfUnit = unitTypes.find(unitType => unitType.id === this.unit.typeId);
  }
}
