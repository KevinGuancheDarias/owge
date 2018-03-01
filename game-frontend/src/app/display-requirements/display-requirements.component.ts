import { BaseComponent } from './../base/base.component';
import { RequirementPojo } from './../shared-pojo/requirement.pojo';
import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-display-requirements',
  templateUrl: './display-requirements.component.html',
  styleUrls: ['./display-requirements.component.less']
})
export class DisplayRequirementsComponent extends BaseComponent implements OnInit {

  public timeImage = 'ui_icons/time.png';
  @Input()
  public requirements: RequirementPojo;

  constructor() {
    super();
  }

  public ngOnInit() {
    this.requireUser();
  }

}
