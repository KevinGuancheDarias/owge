import { Component, OnInit } from '@angular/core';

import { UserStorage, User } from '@owge/core';

import { BaseComponent } from '../base/base.component';
import { MissionService } from '../services/mission.service';
import { UpgradeService } from '../service/upgrade.service';
import { RunningUpgrade } from '../shared-pojo/running-upgrade.pojo';
import { filter, take } from 'rxjs/operators';
import { ObtainedUpgradePojo } from '../shared-pojo/obtained-upgrade.pojo';
import { UnitRunningMission } from '@owge/universe';

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
  public runningUpgrade: RunningUpgrade;
  public relatedObtainedUpgrade: ObtainedUpgradePojo;

  public constructor(
    private _missionService: MissionService,
    private _userStore: UserStorage<User>,
    private _upgradeService: UpgradeService
  ) {
    super();
  }

  public ngOnInit() {
    this.requireUser();
    this._upgradeService.backendRunningUpgradeCheck();
    this._upgradeService.isUpgrading
      .pipe(filter(result => !!result))
      .subscribe(result => {
        this.runningUpgrade = result;
        this._upgradeService.findOneObtained(result.upgrade.id)
          .pipe(take(1)).subscribe(obtainedUpgrade => this.relatedObtainedUpgrade = obtainedUpgrade);
      });
    this._userStore.currentUser
      .subscribe(async () => {
        this.findMyMissions();
        this.findEnemyMissions();
      });
  }

  public findMyMissions(): void {
    this._missionService.findMyRunningMissions().subscribe(myRunningMissions => {
      this.myUnitRunningMissions = myRunningMissions.filter(current => this._missionService.isUnitMission(current));
    });
  }

  public findEnemyMissions(): void {
    this._missionService.findEnemyRunningMissions().subscribe(
      result => this.enemyRunningMissions = result
    );
  }
}
