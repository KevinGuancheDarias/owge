import { Component, OnInit } from '@angular/core';

import { UnitService } from '../../service/unit.service';
import { BaseUnitComponent } from '../../shared/base-unit.component';
import { ObtainedUpgrade, UnitUpgradeRequirements, Upgrade, UpgradeRunningMission } from '@owge/types/universe';
import { UpgradeService } from '../../service/upgrade.service';
import { MissionUtil, ToastrService } from '@owge/core';

interface UpgradeRunningMissionWithPercentage extends UpgradeRunningMission {
  completedPercentage?: number;
}

@Component({
  selector: 'app-unit-requirements',
  templateUrl: './unit-requirements.component.html',
  styleUrls: ['./unit-requirements.component.scss']
})
export class UnitRequirementsComponent extends BaseUnitComponent implements OnInit {
  public unitRequirements: UnitUpgradeRequirements[];
  public runningUpgrade: UpgradeRunningMissionWithPercentage;
  public obtainedUpgrades: { [key: number]: ObtainedUpgrade } = {};

  private _interval: number;

  constructor(private _unitService: UnitService, private _upgradeService: UpgradeService, private _toastrService: ToastrService) {
    super();
    window['globalShit'] = this;
    this._subscriptions.add(
      _upgradeService.findRunningLevelUp().subscribe(runningUpgrade => {
        clearInterval(this._interval);
        if (runningUpgrade) {
          this.runningUpgrade = { ...runningUpgrade };
          this.runningUpgrade.requiredTime *= 1000;
          this._interval = window.setInterval(() => {
            this.runningUpgrade.completedPercentage = MissionUtil.computeProgressPercentage(this.runningUpgrade);
          }, 500);
        } else {
          this.runningUpgrade = null;
        }
      }),
      _upgradeService.findObtained().subscribe(obtainedUpgrades => {
        this.obtainedUpgrades = {};
        obtainedUpgrades.forEach(current => {
          _upgradeService.computeReqiredResources(current, true);
          this.obtainedUpgrades[current.upgrade.id] = current;
        });
      })
    );
  }

  ngOnInit(): void {
    this._subscriptions.add(this._unitService.findUnitUpgradeRequirements().subscribe(result => {
      this.unitRequirements = result;
    }));
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.12
   * @param upgrade
   */
  public clickLevelUp(upgrade: Upgrade): void {
    this._upgradeService.registerLevelUp(this.obtainedUpgrades[upgrade.id]).subscribe(() => {
      this._toastrService.info('APP.UNITS.REQUIREMENTS.UPGRADING', '', {
        name: upgrade.name.replace('DUPLICATED', ''),
        level: this.obtainedUpgrades[upgrade.id].level + 1
      });
    });
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.12
   */
  public cancelLevelUp(): void {
    this._upgradeService.cancelUpgrade().then(() => this._toastrService.info('APP.UNITS.REQUIREMENTS.UPGRADE_CANCELLED'));
  }
}
