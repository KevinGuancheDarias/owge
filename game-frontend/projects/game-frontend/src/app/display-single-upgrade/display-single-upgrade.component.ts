import { Component, OnInit, Input, ViewChild, OnDestroy } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { Improvement, LoggerHelper, UserStorage, User, ScreenDimensionsService, ObservableSubscriptionsHelper } from '@owge/core';
import { WidgetConfirmationDialogComponent } from '@owge/widgets';
import { UniverseGameService, Upgrade, UpgradeRunningMission } from '@owge/universe';

import { BaseComponent } from './../base/base.component';
import { UpgradeService } from './../service/upgrade.service';
import { ObtainedUpgradePojo } from './../shared-pojo/obtained-upgrade.pojo';
import { distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-display-single-upgrade',
  templateUrl: './display-single-upgrade.component.html',
  styleUrls: [
    './display-single-upgrade.component.less',
    './display-single-upgrade.component.scss',
  ]
})
export class DisplaySingleUpgradeComponent extends BaseComponent implements OnInit, OnDestroy {

  @Input()
  public upgrade: Upgrade;

  @Input()
  public obtainedUpgrade: ObtainedUpgradePojo;

  @ViewChild(WidgetConfirmationDialogComponent, { static: true }) public confirmDialog: WidgetConfirmationDialogComponent;

  public runningUpgrade: UpgradeRunningMission;
  public vConfirmDeleteText: string;
  public isDesktop: boolean;

  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);
  private _sdsIdentifier: string;
  private _oldValueRunningUpgrade: UpgradeRunningMission;

  constructor(
    private _upgradeService: UpgradeService,
    private _translateService: TranslateService,
    private _universeGameService: UniverseGameService,
    private _userStore: UserStorage<User>,
    private _screenDimensionsService: ScreenDimensionsService
  ) {
    super();
    this.requireUser();
    this._sdsIdentifier = this._screenDimensionsService.generateIdentifier(this);
  }

  public ngOnInit() {
    this._subscriptions.add(this._screenDimensionsService.hasMinWidth(767, this._sdsIdentifier).subscribe(val => {
      this.isDesktop = val;
    }));
    if (this.obtainedUpgrade) {
      this.upgrade = this.obtainedUpgrade.upgrade;
      this._subscriptions.add(this._userStore.currentUserImprovements.subscribe(improvement =>
        this.obtainedUpgrade = this._upgradeService.computeReqiredResources(this.obtainedUpgrade, true, improvement)
      ));
    }
    this._handleRunningMission();
  }

  public ngOnDestroy(): void {
    this._screenDimensionsService.removeHandler(this._sdsIdentifier);
    super.ngOnDestroy();
  }

  public updateSelectedUpgrade(selected: ObtainedUpgradePojo): void {
    this._upgradeService.registerLevelUp(selected);
  }

  /**
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.2
   * @returns {Promise<void>}
   * @memberof DisplaySingleUpgradeComponent
   */
  public async clickCancelUpgrade(): Promise<void> {
    this.vConfirmDeleteText = await this._translateService.get(
      'UPGRADE.CANCEL_TEXT',
      { upgradeName: this.upgrade.name }
    ).toPromise();
    this.confirmDialog.show();
  }

  public cancelUpgrade(result: boolean): void {
    if (result) {
      this._upgradeService.cancelUpgrade();
    }
  }

  public otherUpgradeAlreadyRunning(): void {
    this.displayError('Ya hay una mejora en curso');
  }

  private _notifyCaller(): void {
    setTimeout(async () => {
      const improvement: Improvement = await this._universeGameService.reloadImprovement();
      this._log.todo(
        [
          'AS upgrade has end, or unit has been deleted, ' +
          'will reload improvements, when websocket becomes available, this should be removed from here',
          improvement
        ]
      );
    }, 5000);
    this.confirmDialog.hide();
  }

  private _handleRunningMission(): void {
    this._subscriptions.add(this._upgradeService.findRunningLevelUp().pipe(distinctUntilChanged()).subscribe(runningUpgrade => {
      this.runningUpgrade = runningUpgrade;
      if (this._oldValueRunningUpgrade && this._oldValueRunningUpgrade.upgrade.id === this.upgrade.id) {
        this._notifyCaller();
      }
      this._oldValueRunningUpgrade = runningUpgrade;
    }));
  }
}
