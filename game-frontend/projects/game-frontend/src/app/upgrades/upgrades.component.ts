import { Component, OnInit, OnDestroy } from '@angular/core';

import { BaseComponent } from './../base/base.component';
import { UpgradeService } from './../service/upgrade.service';
import { UpgradeTypeService } from '../services/upgrade-type.service';
import { UpgradeType } from '../shared/types/upgrade-type.type';
import { ObtainedUpgrade, UpgradeRunningMission } from '@owge/universe';

@Component({
  selector: 'app-upgrades',
  templateUrl: './upgrades.component.html',
  styleUrls: ['./upgrades.component.scss']
})
export class UpgradesComponent extends BaseComponent implements OnInit, OnDestroy {
  private static readonly _SESSION_STORAGE_UPGRADE_TYPE_KEY = 'upgrades.component.unitType';

  public obtainedUpgrades: ObtainedUpgrade[];
  public runningUpgrade: UpgradeRunningMission;
  public upgradeTypes: UpgradeType[];
  public upgradeType: UpgradeType;

  constructor(private _upgradeService: UpgradeService, private _upgradeTypeService: UpgradeTypeService) {
    super();
  }

  ngOnInit() {
    this.upgradeType = JSON.parse(sessionStorage.getItem(UpgradesComponent._SESSION_STORAGE_UPGRADE_TYPE_KEY));
    this._findObtained();
    this._subscriptions.add(this._upgradeTypeService.getUpgradeTypes().subscribe(upgradeTypes => this.upgradeTypes = upgradeTypes));
  }

  public onTypeChange(): void {
    sessionStorage.setItem(UpgradesComponent._SESSION_STORAGE_UPGRADE_TYPE_KEY, JSON.stringify(this.upgradeType));
  }

  private _findObtained(): void {
    this._subscriptions.add(this._upgradeService.findObtained().subscribe(
      obtainedUpgrades => this.obtainedUpgrades = obtainedUpgrades
    ));
  }
}
