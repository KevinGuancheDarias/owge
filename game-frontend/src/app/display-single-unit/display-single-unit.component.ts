import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

import { MEDIA_ROUTES } from './../config/config.pojo';
import { BaseComponent } from './../base/base.component';
import { RunningUnitIntervalInformation, UnitService } from './../service/unit.service';
import { UnitPojo } from './../shared-pojo/unit.pojo';

@Component({
  selector: 'app-display-single-unit',
  templateUrl: './display-single-unit.component.html',
  styleUrls: ['./display-single-unit.component.less']
})
export class DisplaySingleUnitComponent extends BaseComponent implements OnInit {

  @Input()
  public unit: UnitPojo;

  @Input()
  public building: RunningUnitIntervalInformation;

  @Input()
  public withBuildMode = false;

  @Input()
  public withInPlanetMode = false;

  @Output()
  public buildDone: EventEmitter<void> = new EventEmitter();

  /**
   * In planet count
   *
   * @type {number}
   * @memberof DisplaySingleUnitComponent
   */
  @Input()
  public inPlanetCount: number;

  public image: string;
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

  constructor(private _unitService: UnitService) {
    super();
  }

  public ngOnInit() {
    this.requireUser();
    this.image = MEDIA_ROUTES.IMAGES_ROOT + this.unit.image;
    this.unit = this._unitService.computeRequiredResources(this.unit, true, this._count);
    (<any>window).myGlobal = this._count;
  }

  public otherUnitAlreadyRunning(): void {
    this.displayError('Ya hay otras unidades en construcción');
  }

  public cancelUnit(): void {
    this._unitService.cancel(this.building.missionData);
  }

  public buildSelectedUnit(): void {
    this._unitService.registerUnitBuild(this.unit, this.count);
  }

  public noResources(): void {
    this.displayError('No se poseen los recursos necesarios');
  }
}
