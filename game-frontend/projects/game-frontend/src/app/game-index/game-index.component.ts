import { Component, OnInit } from '@angular/core';
import { UserWithAlliance } from '@owge/alliance';
import { User } from '@owge/core';
import { ObtainedUpgrade, TutorialService, UnitRunningMission, UpgradeRunningMission, UserStorage, MissionService } from '@owge/universe';
import { BaseComponent } from '../base/base.component';
import { UpgradeService } from '../service/upgrade.service';
import OBSWebSocket from 'obs-websocket-js';

@Component({
  selector: 'app-game-index',
  templateUrl: './game-index.component.html'
})
export class GameIndexComponent extends BaseComponent<UserWithAlliance> implements OnInit {

  public myUnitRunningMissions: UnitRunningMission<UserWithAlliance>[];
  public enemyRunningMissions: UnitRunningMission<UserWithAlliance>[];
  public alliesRunningMissions: UnitRunningMission<UserWithAlliance>[];
  public unknownRunningMissions: UnitRunningMission<UserWithAlliance>[];
  public runningUpgrade: UpgradeRunningMission;
  public relatedObtainedUpgrade: ObtainedUpgrade;

  public constructor(
    private _missionService: MissionService,
    private _userStore: UserStorage<User>,
    private _upgradeService: UpgradeService,
    private _tutorialService: TutorialService
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
            .subscribe(obtainedUpgrade => this.relatedObtainedUpgrade = obtainedUpgrade);
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
    this._subscriptions.add(this._missionService.findMyRunningMissions<UserWithAlliance>().subscribe(myRunningMissions => {
      this.myUnitRunningMissions = myRunningMissions.filter(current => this._missionService.isUnitMission(current));
      this._tutorialService.triggerTutorialAfterRender();
    }));
  }

  public findEnemyMissions(): void {
    this._subscriptions.add(this._missionService.findEnemyRunningMissions<UserWithAlliance>().subscribe(
      result => {
        this.alliesRunningMissions = [];
        this.enemyRunningMissions = [];
        this.unknownRunningMissions = [];
        result.forEach(mission => {
          if (mission.user && mission.user.alliance && mission.user?.alliance?.id === this.userData?.alliance?.id) {
            this.alliesRunningMissions.push(mission);
          } else if (mission.user) {
            this.enemyRunningMissions.push(mission);
          } else {
            this.unknownRunningMissions.push(mission);
          }
        });
      }
    ));
  }
}
