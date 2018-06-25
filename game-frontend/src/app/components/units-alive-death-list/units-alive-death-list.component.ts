import { Component, Input, AfterViewInit, QueryList, ElementRef, ViewChildren } from '@angular/core';
import { AliveDeathObtainedUnit } from '../../shared/pojos/alive-death-obtained-unit.pojo';
import { UnitPojo } from '../../shared-pojo/unit.pojo';

@Component({
  selector: 'app-units-alive-death-list',
  templateUrl: './units-alive-death-list.component.html',
  styleUrls: ['./units-alive-death-list.component.less']
})
export class UnitsAliveDeathListComponent implements AfterViewInit {


  /**
   * Represents the result of the units involved in an attack, should be loaded <b> externally </b>
   *
   * @type {AliveDeathObtainedUnit[]}
   * @memberof UnitsAliveDeathListComponent
   */
  @Input()
  public units: AliveDeathObtainedUnit[];

  @ViewChildren('unitRoot')
  private _components: QueryList<ElementRef>;

  public ngAfterViewInit(): void {
    setTimeout(() => {
      this._components.forEach(current => {
        const el: HTMLSpanElement = current.nativeElement.querySelector('.card-title span');
        if (el.offsetHeight > 25) {
          const parent = el.parentElement.parentElement.parentElement;
          const interval = setInterval(() => {
            parent.style.width = (parent.offsetHeight + 2) + 'px';
            if (el.offsetHeight < 23) {
              clearInterval(interval);
            }
          });
        }
      });
    }, 150);
  }

  public findUnitImageUrl(unit: UnitPojo): string {
    return UnitPojo.findImagePath(unit);
  }

  public calculateDeathUnits(unit: AliveDeathObtainedUnit): number {
    return unit.initialCount - unit.finalCount;
  }
}
