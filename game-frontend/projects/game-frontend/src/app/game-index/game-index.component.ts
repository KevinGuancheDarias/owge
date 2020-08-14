import { Component, OnInit } from '@angular/core';

import { User } from '@owge/core';

import { BaseComponent } from '../base/base.component';
import { MissionService } from '../services/mission.service';
import { UpgradeService } from '../service/upgrade.service';
import { UnitRunningMission, UpgradeRunningMission, ObtainedUpgrade, UserStorage } from '@owge/universe';
import { take } from 'rxjs/operators';

@Component({
  selector: 'app-game-index',
  templateUrl: './game-index.component.html',
  styleUrls: [
    './game-index.component.less',
    './game-index.component.scss'
  ]
})
export class GameIndexComponent extends BaseComponent implements OnInit {

  public myUnitRunningMissions: UnitRunningMission[];
  public enemyRunningMissions: UnitRunningMission[];
  public runningUpgrade: UpgradeRunningMission;
  public relatedObtainedUpgrade: ObtainedUpgrade;

  public constructor(
    private _missionService: MissionService,
    private _userStore: UserStorage<User>,
    private _upgradeService: UpgradeService
  ) {
    super();
  }

  public ngOnInit() {
    this.requireUser();
    this._subscriptions.add(this._upgradeService.findRunningLevelUp()
      .subscribe(result => {
        this.runningUpgrade = result;
        if (result) {
          this._upgradeService.findOneObtained(result.upgrade.id)
            .pipe(take(1)).subscribe(obtainedUpgrade => this.relatedObtainedUpgrade = obtainedUpgrade);
        } else {
          this.relatedObtainedUpgrade = null;
        }
      })
    );
    this._userStore.currentUser
      .subscribe(async () => {
        this.findMyMissions();
        this.findEnemyMissions();
      });
  }

  public findMyMissions(): void {
    this._subscriptions.add(this._missionService.findMyRunningMissions().subscribe(myRunningMissions => {
      this.myUnitRunningMissions = myRunningMissions.filter(current => this._missionService.isUnitMission(current));
    }));
  }

  public findEnemyMissions(): void {
    this._subscriptions.add(this._missionService.findEnemyRunningMissions().subscribe(
      result => this.enemyRunningMissions = result
    ));
  }
}
