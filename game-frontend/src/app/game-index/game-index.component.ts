import { Component, OnInit } from '@angular/core';

import { BaseComponent } from '../base/base.component';
import { AnyRunningMission } from '../shared/types/any-running-mission.type';
import { MissionService } from '../services/mission.service';
import { UnitRunningMission } from '../shared/types/unit-running-mission.type';

@Component({
  selector: 'app-game-index',
  templateUrl: './game-index.component.html',
  styleUrls: ['./game-index.component.less']
})
export class GameIndexComponent extends BaseComponent implements OnInit {

  public myUnitRunningMissions: AnyRunningMission[];
  public enemyRunningMissions: UnitRunningMission[];

  public constructor(private _missionService: MissionService) {
    super();
  }

  public ngOnInit() {
    this.requireUser();
    this.loginSessionService
      .findLoggedInUserData()
      .subscribe(async () => {
        this._missionService.findMyRunningMissions().subscribe(myRunningMissions => {
          this.myUnitRunningMissions = myRunningMissions.filter(current => this._missionService.isUnitMission(current));
        });
        this.enemyRunningMissions = await this._missionService.findEnemyRunningMissions().toPromise();
      });
  }
}
