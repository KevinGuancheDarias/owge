import { Component, Input, AfterViewInit, QueryList, ElementRef, ViewChildren } from '@angular/core';
import { AliveDeathObtainedUnit } from '../../shared/pojos/alive-death-obtained-unit.pojo';
import { BaseComponent } from '../../base/base.component';

@Component({
  selector: 'app-units-alive-death-list',
  templateUrl: './units-alive-death-list.component.html',
  styleUrls: ['./units-alive-death-list.component.less']
})
export class UnitsAliveDeathListComponent extends BaseComponent implements AfterViewInit {


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
    this.autoSpanCard(this._components, '.card-title span', el => {
      return el.parentElement.parentElement.parentElement;
    });
  }

  public calculateDeathUnits(unit: AliveDeathObtainedUnit): number {
    return unit.initialCount - unit.finalCount;
  }
}
