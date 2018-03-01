import { RunningUpgrade } from './../shared-pojo/running-upgrade.pojo';
import { ObtainedUpgradePojo } from './../shared-pojo/obtained-upgrade.pojo';
import { BaseComponent } from './../base/base.component';
import { UpgradeService } from './../service/upgrade.service';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-upgrades',
  templateUrl: './upgrades.component.html',
  styleUrls: ['./upgrades.component.less']
})
export class UpgradesComponent extends BaseComponent implements OnInit {

  public obtainedUpgrades: ObtainedUpgradePojo[];
  public runningUpgrade: RunningUpgrade;

  constructor(private _upgradeService: UpgradeService) {
    super();
  }

  ngOnInit() {
    this.findObtained();
    this._upgradeService.backendRunningUpgradeCheck();
    this.isUpgrading();
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
    console.log('Esto se ha hecho');
    this.findObtained();
  }

}
