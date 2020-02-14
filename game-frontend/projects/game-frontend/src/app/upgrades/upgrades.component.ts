import { Component, OnInit } from '@angular/core';

import { RunningUpgrade } from './../shared-pojo/running-upgrade.pojo';
import { ObtainedUpgradePojo } from './../shared-pojo/obtained-upgrade.pojo';
import { BaseComponent } from './../base/base.component';
import { UpgradeService } from './../service/upgrade.service';
import { UpgradeTypeService } from '../services/upgrade-type.service';
import { UpgradeType } from '../shared/types/upgrade-type.type';

@Component({
  selector: 'app-upgrades',
  templateUrl: './upgrades.component.html',
  styleUrls: ['./upgrades.component.scss']
})
export class UpgradesComponent extends BaseComponent implements OnInit {
  private static readonly _SESSION_STORAGE_UPGRADE_TYPE_KEY = 'upgrades.component.unitType';

  public obtainedUpgrades: ObtainedUpgradePojo[];
  public runningUpgrade: RunningUpgrade;
  public upgradeTypes: UpgradeType[];
  public upgradeType: UpgradeType;

  constructor(private _upgradeService: UpgradeService, private _upgradeTypeService: UpgradeTypeService) {
    super();
  }

  ngOnInit() {
    this.upgradeType = JSON.parse(sessionStorage.getItem(UpgradesComponent._SESSION_STORAGE_UPGRADE_TYPE_KEY));
    this.findObtained();
    this._upgradeService.backendRunningUpgradeCheck();
    this.isUpgrading();
    this._upgradeTypeService.getUpgradeTypes().subscribe(upgradeTypes => this.upgradeTypes = upgradeTypes);
  }

  public onTypeChange(): void {
    sessionStorage.setItem(UpgradesComponent._SESSION_STORAGE_UPGRADE_TYPE_KEY, JSON.stringify(this.upgradeType));
  }

  public isUpgrading(): void {
    this._upgradeService.isUpgrading.subscribe(value => {
      this.runningUpgrade = value;
    });
  }

  public findObtained(): void {
    this._upgradeService.findObtained().subscribe(
      obtainedUpgrades => this.obtainedUpgrades = obtainedUpgrades,
      error => this.displayError(error)
    );
  }

  public onRunningUpgradeDone(): void {
    this.findObtained();
  }

}
