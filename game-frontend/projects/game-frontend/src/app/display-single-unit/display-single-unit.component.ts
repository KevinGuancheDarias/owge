import { Component, Input, OnDestroy, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import { ModalComponent, UnitType, User } from '@owge/core';
import { UserWithFaction } from '@owge/faction';
import { ImprovementUtil, ObtainedUnit, Unit, UnitBuildRunningMission, UserStorage } from '@owge/universe';
import { WidgetConfirmationDialogComponent } from '@owge/widgets';
import { BehaviorSubject, Subscription } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { UnitTypeService } from '../services/unit-type.service';
import { CriticalAttackInformation } from '../types/critical-attack-information.type';
import { BaseComponent } from './../base/base.component';
import { UnitService } from './../service/unit.service';



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
export class DisplaySingleUnitComponent extends BaseComponent<UserWithFaction> implements OnInit, OnDestroy {

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

  public selectedView: ValidViews;
  public numberToDelete: number;
  public isDescriptionDisplayed = false;
  public moreAttack: number;
  public moreShield: number;
  public moreHealth: number;
  public moreCharge: number;
  public moreSpeed: number;
  public unitTypes: UnitType[];
  public unitType: UnitType;
  public attackableUnitTypes: AttackableUnitType[];
  public criticalAttackInformations: CriticalAttackInformation[];
  public isDefaultCriticalDisplayed = false;

  public get count(): any {
    return this._count.value;
  }

  public set count(value: any) {
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

  constructor(
    private _unitService: UnitService,
    private _unitTypeService: UnitTypeService,
    private _userStore: UserStorage<User>
  ) {
    super();
  }

  public ngOnInit() {
    this.requireUser(() => {
      if (this._improvementsSubscription) {
        this._improvementsSubscription.unsubscribe();
        delete this._improvementsSubscription;
      }
      this._improvementsSubscription = this._userStore.currentUserImprovements.subscribe(async improvement => {
        this.moreCharge = improvement.moreChargeCapacity;
        const unitTypes: UnitType[] = await this._unitTypeService.getUnitTypes().pipe(
          filter(result => !!result),
          take(1)
        ).toPromise();
        const unitTypeOfUnit = unitTypes.find(unitType => unitType.id === this.unit.typeId);
        if (unitTypeOfUnit) {
          this.moreAttack = ImprovementUtil.findUnitTypeImprovement(improvement, 'ATTACK', unitTypeOfUnit);
          this.moreShield = ImprovementUtil.findUnitTypeImprovement(improvement, 'SHIELD', unitTypeOfUnit);
          this.moreHealth = ImprovementUtil.findUnitTypeImprovement(improvement, 'DEFENSE', unitTypeOfUnit);
          this.moreSpeed = ImprovementUtil.findUnitTypeImprovement(improvement, 'SPEED', unitTypeOfUnit);
        } else {
          console.warn(`Unit with id ${this.unit.id} doesn't have a unitType`);
        }
      });
    });
    this._unitTypeService.getUnitTypes().subscribe(val => this.unitTypes = val);
    this.unit = this._unitService.computeRequiredResources(this.unit, true, this._count);
    this.selectedView = this.defaultView;
  }

  public otherUnitAlreadyRunning(): void {
    this.displayError('Ya hay otras unidades en construcción');
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.20
   */
  public clickOpenUnitInfo(): void {
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

  public cancelBuild(confirm: boolean): void {
    if (confirm) {
      this._unitService.cancel(this.building).pipe(take(1)).subscribe();
    }
  }

  public async deleteUnits(): Promise<void> {
    if (await this.displayConfirm('Are you sure you want to delete the unit?')) {
      await this._doWithLoading(this._unitService.deleteObtainedUnit(this.obtainedUnit, this.numberToDelete));
    }
  }

  public buildSelectedUnit(): void {
    this._unitService.registerUnitBuild(this.unit, this.count);
  }

  public noResources(): void {
    this.displayError('No se poseen los recursos necesarios');
  }

  public isValidDeletion(): boolean {
    return this.numberToDelete && this.numberToDelete <= this.inPlanetCount;
  }

  public canBuild(): boolean {
    return this.unit.requirements.runnable && this._unitTypeService.hasAvailable(this.unit.typeId, this.count);
  }

  /**
   * If unit is unique should not allow more than one as <i>count</i> value
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @returns
   * @memberof DisplaySingleUnitComponent
   */
  public isValidCount(): boolean {
    return (this.unit.isUnique && this.count === 1) || !this.unit.isUnique;
  }

}
