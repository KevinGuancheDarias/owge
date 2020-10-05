import { Component, OnInit, Input, OnChanges, OnDestroy } from '@angular/core';
import { MilisToDaysHoursMinutesSeconds, DateTimeUtil } from '../shared/util/date-time.util';
import { UserWithFaction } from '@owge/faction';
import { ResourceRequirements, UserStorage } from '@owge/universe';
import { ScreenDimensionsService } from '@owge/core';

@Component({
  selector: 'app-display-requirements',
  templateUrl: './display-requirements.component.html',
  styleUrls: [
    './display-requirements.component.less',
    './display-requirements.component.scss'
  ]
})
export class DisplayRequirementsComponent implements OnInit, OnChanges, OnDestroy {
  private static readonly _INTENTIONAL_DELAY = 3000;

  public userData: UserWithFaction;
  public timeImage = 'ui_icons/time.png';

  @Input()
  public requirements: ResourceRequirements;

  public parsedRequiredTime: MilisToDaysHoursMinutesSeconds;
  public limitedMax = false;

  private _id = `requirements_${new Date().getTime()}`;

  constructor(private _userStore: UserStorage<UserWithFaction>, private _sdsService: ScreenDimensionsService) { }

  public ngOnInit() {
    this._sdsService.hasMinWidth(767, this._id).subscribe(val => this.limitedMax = val);
    this._userStore.currentUser.subscribe(val => this.userData = val);
  }

  public ngOnChanges(): void {
    this.parsedRequiredTime =
      DateTimeUtil.milisToDaysHoursMinutesSeconds(
        (this.requirements.requiredTime * 1000) + DisplayRequirementsComponent._INTENTIONAL_DELAY
      );
  }

  public ngOnDestroy(): void {
    this._sdsService.removeHandler(this._id);
  }
}
