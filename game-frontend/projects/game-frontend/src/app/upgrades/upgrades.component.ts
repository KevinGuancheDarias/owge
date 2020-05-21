import { Component, OnInit, OnDestroy } from '@angular/core';

import { BaseComponent } from './../base/base.component';
import { UpgradeService } from './../service/upgrade.service';
import { UpgradeTypeService } from '../services/upgrade-type.service';
import { UpgradeType } from '../shared/types/upgrade-type.type';
import { ObtainedUpgrade, UpgradeRunningMission } from '@owge/universe';
import { ObservableSubscriptionsHelper } from '@owge/core';

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

  private _suscriptions: ObservableSubscriptionsHelper = new ObservableSubscriptionsHelper;

  constructor(private _upgradeService: UpgradeService, private _upgradeTypeService: UpgradeTypeService) {
    super();
  }

  ngOnInit() {
    this.upgradeType = JSON.parse(sessionStorage.getItem(UpgradesComponent._SESSION_STORAGE_UPGRADE_TYPE_KEY));
    this._findObtained();
    this._suscriptions.add(this._upgradeTypeService.getUpgradeTypes().subscribe(upgradeTypes => this.upgradeTypes = upgradeTypes));
  }

  public ngOnDestroy(): void {
    this._suscriptions.unsubscribeAll();
  }

  public onTypeChange(): void {
    sessionStorage.setItem(UpgradesComponent._SESSION_STORAGE_UPGRADE_TYPE_KEY, JSON.stringify(this.upgradeType));
  }

  private _findObtained(): void {
    this._suscriptions.add(this._upgradeService.findObtained().subscribe(
      obtainedUpgrades => this.obtainedUpgrades = obtainedUpgrades,
      error => this.displayError(error)
    ));
  }
}
