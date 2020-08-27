import { Component, OnInit, Input, ViewEncapsulation, OnDestroy } from '@angular/core';
import { BehaviorSubject, Subscription } from 'rxjs';

import { User } from '@owge/core';
import { UnitType, Unit, UnitBuildRunningMission, ObtainedUnit, UserStorage, ImprovementUtil } from '@owge/universe';

import { BaseComponent } from './../base/base.component';
import { UnitService } from './../service/unit.service';
import { UnitTypeService } from '../services/unit-type.service';
import { filter, take } from 'rxjs/operators';

export type validViews = 'requirements' | 'attributes';

@Component({
  selector: 'app-display-single-unit',
  templateUrl: './display-single-unit.component.html',
  styleUrls: ['./display-single-unit.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DisplaySingleUnitComponent extends BaseComponent implements OnInit, OnDestroy {

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
   * @type {validViews}
   * @memberof DisplaySingleUnitComponent
   */
  @Input()
  public defaultView: validViews = 'attributes';

  public selectedView: validViews;
  public numberToDelete: number;
  public isDescriptionDisplayed = false;
  public moreAttack: number;
  public moreShield: number;
  public moreHealth: number;
  public moreCharge: number;
  public unitTypes: UnitType[];

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

  public cancelUnit(): void {
    this._unitService.cancel(this.building);
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
   * @returns {boolean}
   * @memberof DisplaySingleUnitComponent
   */
  public isValidCount(): boolean {
    return (this.unit.isUnique && this.count === 1) || !this.unit.isUnique;
  }

}
