import { Component, OnInit, Input, Output, EventEmitter, ViewEncapsulation } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

import { Improvement, LoggerHelper, UserStorage, User, ImprovementUtil } from '@owge/core';
import { UniverseGameService, UnitType, Unit } from '@owge/universe';

import { BaseComponent } from './../base/base.component';
import { RunningUnitIntervalInformation, UnitService } from './../service/unit.service';
import { ObtainedUnit } from '../shared-pojo/obtained-unit.pojo';
import { UnitTypeService } from '../services/unit-type.service';

export type validViews = 'requirements' | 'attributes';

@Component({
  selector: 'app-display-single-unit',
  templateUrl: './display-single-unit.component.html',
  styleUrls: ['./display-single-unit.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DisplaySingleUnitComponent extends BaseComponent implements OnInit {

  @Input()
  public unit: Unit;

  @Input() isCompactView = false;

  @Input()
  public building: RunningUnitIntervalInformation;

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

  @Output()
  public buildDone: EventEmitter<void> = new EventEmitter();

  @Output()
  public delete: EventEmitter<void> = new EventEmitter();

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

  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);

  constructor(
    private _unitService: UnitService,
    private _unitTypeService: UnitTypeService,
    private _universeGameService: UniverseGameService,
    private _userStore: UserStorage<User>
  ) {
    super();
  }

  public ngOnInit() {
    this.requireUser();
    this._unitTypeService.getUnitTypes().subscribe(val => this.unitTypes = val);
    this._userStore.currentUserImprovements.subscribe(improvement => {
      this.moreCharge = improvement.moreChargeCapacity;
      this.moreAttack = ImprovementUtil.findUnitTypeImprovement(improvement, 'ATTACK', this.unit.typeId);
      this.moreShield = ImprovementUtil.findUnitTypeImprovement(improvement, 'SHIELD', this.unit.typeId);
      this.moreHealth = ImprovementUtil.findUnitTypeImprovement(improvement, 'DEFENSE', this.unit.typeId);
    });
    this.unit = this._unitService.computeRequiredResources(this.unit, true, this._count);
    this.selectedView = this.defaultView;
  }

  public otherUnitAlreadyRunning(): void {
    this.displayError('Ya hay otras unidades en construcción');
  }

  public cancelUnit(): void {
    this._unitService.cancel(this.building.missionData);
  }

  public async deleteUnits(): Promise<void> {
    if (await this.displayConfirm('Are you sure you want to delete the unit?')) {
      this.obtainedUnit.count = this.numberToDelete;
      await this._doWithLoading(this._unitService.deleteObtainedUnit(this.obtainedUnit));
      await this._reloadImprovement(this.obtainedUnit.unit);
      this.delete.emit();
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

  /**
   * Runs when the build of the unit is done
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   */
  public async onBuildDone(unit: Unit): Promise<void> {
    await this._reloadImprovement(unit);
    this.buildDone.emit();
  }

  private async _reloadImprovement(unit: Unit): Promise<void> {
    if (!unit || unit.improvement) {
      const improvement: Improvement = await this._universeGameService.reloadImprovement();
      this._log.todo(
        [
          'AS unit build has end, or unit has been deleted, ' +
          'will reload improvements, when websocket becomes available, this should be removed from here',
          improvement
        ]
      );
    }
  }
}
