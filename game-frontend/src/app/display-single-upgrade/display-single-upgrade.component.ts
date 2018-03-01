import { BaseComponent } from './../base/base.component';
import { RunningUpgrade } from './../shared-pojo/running-upgrade.pojo';
import { UpgradeService } from './../service/upgrade.service';
import { ObtainedUpgradePojo } from './../shared-pojo/obtained-upgrade.pojo';
import { MEDIA_ROUTES } from './../config/config.pojo';
import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { Upgrade } from './../shared-pojo/upgrade.pojo';

@Component({
  selector: 'app-display-single-upgrade',
  templateUrl: './display-single-upgrade.component.html',
  styleUrls: ['./display-single-upgrade.component.less']
})
export class DisplaySingleUpgradeComponent extends BaseComponent implements OnInit {

  @Input()
  public upgrade: Upgrade;

  @Input()
  public obtainedUpgrade: ObtainedUpgradePojo;

  @Output()
  public onRunningUpgradeDone: EventEmitter<{}> = new EventEmitter();

  public image: string;

  public runningUpgrade: RunningUpgrade;

  constructor(private _upgradeService: UpgradeService) {
    super();
    this.requireUser();
  }

  public ngOnInit() {
    if (this.obtainedUpgrade) {
      this.upgrade = this.obtainedUpgrade.upgrade;
      this.obtainedUpgrade = this._upgradeService.computeReqiredResources(this.obtainedUpgrade, true);
    }
    this.image = MEDIA_ROUTES.IMAGES_ROOT + this.upgrade.image;
    this._syncIsUpgrading();
  }

  public updateSelectedUpgrade(selected: ObtainedUpgradePojo): void {
    this._upgradeService.registerLevelUp(selected);
  }

  public cancelUpgrade(): void {
    this._upgradeService.cancelUpgrade();
  }

  public otherUpgradeAlreadyRunning(): void {
    this.displayError('Ya hay una mejora en curso');
  }

  /**
   * When a it's the running upgrade an it's done, will fire an event
   *
   * @author Kevin Guanche Darias
   */
  public notifyCaller(): void {
    this.onRunningUpgradeDone.emit();
  }

  private _syncIsUpgrading(): void {
    this._upgradeService.isUpgrading.subscribe(runningUpgrade => this.runningUpgrade = runningUpgrade);
  }

}
