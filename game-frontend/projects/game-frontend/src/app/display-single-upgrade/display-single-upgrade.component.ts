import { Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ModalComponent, ScreenDimensionsService } from '@owge/core';
import { User } from '@owge/types/core';
import { UserWithFaction } from '@owge/types/faction';
import { TutorialService, UserStorage } from '@owge/universe';
import { ObtainedUpgrade, Upgrade, UpgradeRunningMission } from '@owge/types/universe';
import { WidgetConfirmationDialogComponent } from '@owge/widgets';
import { distinctUntilChanged } from 'rxjs/operators';
import { BaseComponent } from './../base/base.component';
import { UpgradeService } from './../service/upgrade.service';



@Component({
  selector: 'app-display-single-upgrade',
  templateUrl: './display-single-upgrade.component.html',
  styleUrls: [ './display-single-upgrade.component.scss']
})
export class DisplaySingleUpgradeComponent extends BaseComponent<UserWithFaction> implements OnInit, OnDestroy {

  @Input()
  public upgrade: Upgrade;

  @Input()
  public obtainedUpgrade: ObtainedUpgrade;

  @ViewChild(ModalComponent) public modal: ModalComponent;
  @ViewChild(WidgetConfirmationDialogComponent, { static: true }) public confirmDialog: WidgetConfirmationDialogComponent;

  public runningUpgrade: UpgradeRunningMission;
  public vConfirmDeleteText: string;
  public isDesktop: boolean;
  public selectedView: 'requirements' | 'improvements' = 'requirements';

  private _sdsIdentifier: string;
  private _oldValueRunningUpgrade: UpgradeRunningMission;

  constructor(
    private _upgradeService: UpgradeService,
    private _translateService: TranslateService,
    private _userStore: UserStorage<User>,
    private _screenDimensionsService: ScreenDimensionsService,
    private _tutorialService: TutorialService
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

  public updateSelectedUpgrade(selected: ObtainedUpgrade): void {
    this._upgradeService.registerLevelUp(selected).subscribe(() => this._tutorialService.triggerTutorialAfterRender());
  }

  /**
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.2
   * @returns
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
