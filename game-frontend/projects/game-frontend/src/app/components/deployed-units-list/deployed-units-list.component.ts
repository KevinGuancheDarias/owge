import { Component, Input, OnChanges, OnInit, Output, EventEmitter } from '@angular/core';

import { UnitType } from '@owge/universe';

import { ObtainedUnit } from '../../shared-pojo/obtained-unit.pojo';
import { SelectedUnit } from '../../shared/types/selected-unit.type';
import { UnitTypeService } from '../../services/unit-type.service';

@Component({
  selector: 'app-deployed-units-list',
  templateUrl: './deployed-units-list.component.html',
  styleUrls: ['./deployed-units-list.component.less', './deployed-units-list.component.scss']
})
export class DeployedUnitsListComponent implements OnChanges {

  @Input()
  public obtainedUnits: ObtainedUnit[];

  /**
   * If can select the unit to be used
   *
   * @type {boolean}
   * @memberof DeployedUnitsListComponent
   */
  @Input()
  public selectable = false;

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

  public unitTypes: UnitType[] = [];
  public parsedObtained: {
    [key: number]: {
      typeName: string,
      obtainedUnits?: ObtainedUnit[],
      selectedCounts: number[],
      allSelected: boolean
    }
  } = {};

  constructor(private _unitTypeService: UnitTypeService) { }

  public ngOnChanges() {
    this.parsedObtained = {};
    if (this.obtainedUnits) {
      this.obtainedUnits
        .filter(obtainedUnit => obtainedUnit.unit.typeId)
        .forEach(obtainedUnit => {
          const typeIndex: number = obtainedUnit.unit.typeId;
          if (this.parsedObtained[typeIndex]) {
            this.parsedObtained[typeIndex].selectedCounts.push(null);
            this.parsedObtained[typeIndex].obtainedUnits.push(obtainedUnit);
          } else {
            this.parsedObtained[typeIndex] = {
              typeName: obtainedUnit.unit.typeName,
              obtainedUnits: [obtainedUnit],
              selectedCounts: [null],
              allSelected: false
            };
          }
        });
    }
  }


  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   */
  public clickAllOfType(unitTypeId): void {
    const currentAllSelected = this.parsedObtained[unitTypeId].allSelected;
    this.parsedObtained[unitTypeId].obtainedUnits
      .forEach((obtainedUnit, i) => this.parsedObtained[unitTypeId].selectedCounts[i] = currentAllSelected ? 0 : obtainedUnit.count);
    this.parsedObtained[unitTypeId].allSelected = !currentAllSelected;
  }

  public async selectionChanged(): Promise<void> {
    const selectedCounts: SelectedUnit[] = [];
    Object.keys(this.parsedObtained).forEach(typeId => {
      this.parsedObtained[+typeId].obtainedUnits.forEach((obtainedUnit, index) => {
        selectedCounts.push({
          unit: obtainedUnit.unit,
          count: this.parsedObtained[+typeId].selectedCounts[index]
        });
      });
    });
    this.selection.emit(selectedCounts.filter(current => current.count));
    const ids: number[] = selectedCounts.map<number>(
      current => current && current.count ? current.unit.typeId : null
    ).filter(current => current !== null);
    this.unitTypesOfSelection.emit(await this._unitTypeService.idsToUnitTypes(...Array.from(new Set(ids))));
  }
}
