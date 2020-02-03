import { BaseComponent } from './../base/base.component';
import { RequirementPojo } from './../shared-pojo/requirement.pojo';
import { Component, OnInit, Input, OnChanges } from '@angular/core';
import { MilisToDaysHoursMinutesSeconds, DateTimeUtil } from '../shared/util/date-time.util';

@Component({
  selector: 'app-display-requirements',
  templateUrl: './display-requirements.component.html',
  styleUrls: [
    './display-requirements.component.less',
    './display-requirements.component.scss'
  ]
})
export class DisplayRequirementsComponent extends BaseComponent implements OnInit, OnChanges {

  public timeImage = 'ui_icons/time.png';

  @Input()
  public requirements: RequirementPojo;

  public parsedRequiredTime: MilisToDaysHoursMinutesSeconds;
  constructor() {
    super();
  }

  public ngOnInit() {
    this.requireUser();
  }

  public ngOnChanges(): void {
    this.parsedRequiredTime = DateTimeUtil.milisToDaysHoursMinutesSeconds(this.requirements.requiredTime * 1000);
  }
}
