import { Component, ViewEncapsulation, Input, OnChanges, OnInit, Output, EventEmitter } from '@angular/core';
import { ObtainedUnit } from '../../shared-pojo/obtained-unit.pojo';
import { UnitPojo } from '../../shared-pojo/unit.pojo';
import { SelectedUnit } from '../../shared/types/selected-unit.type';

@Component({
  selector: 'app-deployed-units-list',
  templateUrl: './deployed-units-list.component.html',
  styleUrls: ['./deployed-units-list.component.less']
})
export class DeployedUnitsListComponent implements OnInit, OnChanges {

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

  @Output()
  public selection: EventEmitter<SelectedUnit[]> = new EventEmitter();

  public selectedCounts: number[];

  constructor() { }

  public ngOnInit() {
    this.ngOnChanges();
  }

  public ngOnChanges() {
    if (this.obtainedUnits) {
      this.selectedCounts = this.obtainedUnits.map(() => 0);
    }
  }

  public findUnitImageUrl(unit: UnitPojo): string {
    return UnitPojo.findImagePath(unit);
  }

  public selectionChanged(): void {
    this.selection.emit(
      this.selectedCounts.map<SelectedUnit>((current, index) => {
        return {
          id: this.obtainedUnits[index].unit.id,
          count: current
        };
      }).filter(current => current.count)
    );
  }
}
