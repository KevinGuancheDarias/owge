import { Component, OnInit, Input, OnChanges } from '@angular/core';
import { MilisToDaysHoursMinutesSeconds, DateTimeUtil } from '../shared/util/date-time.util';
import { UserStorage } from '@owge/core';
import { UserWithFaction } from '@owge/faction';
import { ResourceRequirements } from '@owge/universe';

@Component({
  selector: 'app-display-requirements',
  templateUrl: './display-requirements.component.html',
  styleUrls: [
    './display-requirements.component.less',
    './display-requirements.component.scss'
  ]
})
export class DisplayRequirementsComponent implements OnInit, OnChanges {
  private static readonly _INTENTIONAL_DELAY = 3000;

  public userData: UserWithFaction;
  public timeImage = 'ui_icons/time.png';

  @Input()
  public requirements: ResourceRequirements;

  public parsedRequiredTime: MilisToDaysHoursMinutesSeconds;
  constructor(private _userStore: UserStorage<UserWithFaction>) {
  }

  public ngOnInit() {
    this._userStore.currentUser.subscribe(val => this.userData = val);
  }

  public ngOnChanges(): void {
    this.parsedRequiredTime =
      DateTimeUtil.milisToDaysHoursMinutesSeconds(
        (this.requirements.requiredTime * 1000) + DisplayRequirementsComponent._INTENTIONAL_DELAY
      );
  }
}
