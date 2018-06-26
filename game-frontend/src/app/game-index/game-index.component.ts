import { Component, OnInit } from '@angular/core';

import { BaseComponent } from '../base/base.component';
import { AnyRunningMission } from '../shared/types/any-running-mission.type';
import { MissionService } from '../services/mission.service';

@Component({
  selector: 'app-game-index',
  templateUrl: './game-index.component.html',
  styleUrls: ['./game-index.component.less']
})
export class GameIndexComponent extends BaseComponent implements OnInit {

  public myUnitRunningMissions: AnyRunningMission[];

  public constructor(private _missionService: MissionService) {
    super();
  }

  public ngOnInit() {
    this.requireUser();
    this.loginSessionService
      .findLoggedInUserData()
      .subscribe(async () => {
        const myRunningMissions = await this._missionService.findMyRunningMissions().toPromise();
        this.myUnitRunningMissions = myRunningMissions.filter(current => this._missionService.isUnitMission(current));
      });
  }
}
