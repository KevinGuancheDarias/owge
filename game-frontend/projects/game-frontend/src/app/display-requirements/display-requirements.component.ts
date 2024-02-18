import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ScreenDimensionsService } from '@owge/core';
import { UserWithFaction } from '@owge/types/faction';
import { ResourceRequirements, UserStorage } from '@owge/universe';
import { DateTimeUtil, MilisToDaysHoursMinutesSeconds } from '../shared/util/date-time.util';

@Component({
  selector: 'app-display-requirements',
  templateUrl: './display-requirements.component.html',
  styleUrls: ['./display-requirements.component.scss']
})
export class DisplayRequirementsComponent implements OnInit, OnChanges, OnDestroy {

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
        (this.requirements.requiredTime * 1000)
      );
  }

  public ngOnDestroy(): void {
    this._sdsService.removeHandler(this._id);
  }
}
