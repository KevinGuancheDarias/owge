import { Component, Input, OnChanges, OnInit, Output, EventEmitter, SimpleChanges } from '@angular/core';

import { ObtainedUnit } from '@owge/universe';

import { SelectedUnit } from '../../shared/types/selected-unit.type';
import { UnitTypeService } from '../../services/unit-type.service';
import { ToastrService, UnitType } from '@owge/core';

interface UnitsForEachUser {
  username: string;
  obtainedUnits: ObtainedUnit[];
}

@Component({
  selector: 'app-deployed-units-list',
  templateUrl: './deployed-units-list.component.html',
  styleUrls: ['./deployed-units-list.component.less', './deployed-units-list.component.scss']
})
export class DeployedUnitsListComponent implements OnInit, OnChanges {

  @Input()
  public obtainedUnits: ObtainedUnit[];

  @Input() public showUsername = false;

  /**
   * If can select the unit to be used
   *
   * @type {boolean}
   * @memberof DeployedUnitsListComponent
   */
  @Input()
  public selectable = false;

  @Input() public selectAllNotAvailableText = 'APP.DEPLOYED_UNIT_LIST.DEFAULT_NOT_AVAILABLE';
  @Input() public filterForAll: (unit: ObtainedUnit) => Promise<boolean>;

  /**
   * Optional, if specified, will display the sustractionf of obtainedUnit.count - <i>finalCount</i>
   *
   * @type {number}
   * @memberof DeployedUnitsListComponent
   */
  @Input()
  public finalCount: number;

  /**
   * Allow to choose if should use tiny cards or not
   *
   * @since 0.8.1
   */
  @Input() public useTiny = false;

  @Output()
  public selection: EventEmitter<SelectedUnit[]> = new EventEmitter();

  @Output()
  public unitTypesOfSelection: EventEmitter<UnitType[]> = new EventEmitter();

  public selectedCounts: number[];
  public unitTypes: UnitType[] = [];
  public areAllSelected = false;
  public unitsForEachUser: Array<UnitsForEachUser> = [];

  constructor(private _unitTypeService: UnitTypeService, private _toastrService: ToastrService) { }

  public ngOnInit() {
    this.ngOnChanges(null);
  }

  public ngOnChanges(changes: SimpleChanges) {
    if (changes && changes.obtainedUnits.currentValue) {
      this.areAllSelected = false;
      this.selectedCounts = this.obtainedUnits.map(() => null);
    }
    if (changes && changes.showUsername?.currentValue && this.obtainedUnits) {
      this.unitsForEachUser = [];
      this.obtainedUnits.forEach(unit => {
        const currentObject: UnitsForEachUser = this.unitsForEachUser.find(current => current.username === unit.username);
        if (currentObject) {
          currentObject.obtainedUnits.push(unit);
        } else {
          this.unitsForEachUser.push({
            username: unit.username,
            obtainedUnits: [
              unit
            ]
          });
        }
      });
    }
  }

  public async selectionChanged(): Promise<void> {
    this.areAllSelected = false;
    this.selection.emit(
      this.selectedCounts.map<SelectedUnit>((current, index) => {
        return {
          id: this.obtainedUnits[index].unit.id,
          count: current,
          unit: this.obtainedUnits[index].unit
        };
      }).filter(current => current.count)
    );
    const ids: number[] = this.selectedCounts.map<number>(
      (current, index) => current ? this.obtainedUnits[index].unit.typeId : null
    ).filter(current => current !== null);
    this.unitTypesOfSelection.emit(await this._unitTypeService.idsToUnitTypes(...ids));
  }

  public async clickSelectAll(): Promise<void> {
    if (this.filterForAll) {
      let selectionChanged = false;
      await Promise.all(this.obtainedUnits.map(async (obtainedUnit, i) => {
        if (await this.filterForAll(obtainedUnit)) {
          selectionChanged = true;
          this.selectedCounts[i] = obtainedUnit.count;
        }
      }));
      if (selectionChanged) {
        this.selectionChanged();
      }
      this.areAllSelected = true;
    } else {
      this._toastrService.error(this.selectAllNotAvailableText);
    }
  }

  public clickUnselectAll(): void {
    this.selectedCounts = this.obtainedUnits.map(() => null);
    this.areAllSelected = false;
  }
}
