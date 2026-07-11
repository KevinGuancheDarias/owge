import { Component, OnInit, ViewChild } from '@angular/core';
import { LoadingService } from '@owge/core';
import { UniverseGameService } from '@owge/universe';
import { WidgetConfirmationDialogComponent } from '@owge/widgets';
import { take } from 'rxjs/operators';
import { AdminSystemMessageService } from '../../services/admin-system-message.service';

@Component({
  selector: 'app-index',
  templateUrl: './index.component.html',
  styleUrls: ['./index.component.less']
})
export class IndexComponent {

  public runningHangMissions = false;
  public runningUnlockRepair = false;

  constructor(
    private _loadingService: LoadingService,
    private _universeGameService: UniverseGameService,
    private _adminSystemMessageService: AdminSystemMessageService
  ) { }


  /**
   *
   * @todo Remove this when the backend is clever enough to clear the caches when required <br>
   *  ie: Because an unit was modified, should clear all its information
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   */
  public confirmDelete(confirmResult: boolean): void {
    if (confirmResult) {
      this._loadingService.addPromise(
        this._universeGameService.requestWithAutorizationToContext('admin', 'delete', 'cache/drop-all').pipe(take(1)).toPromise()
      );
    } else {
      alert('Clever decision!!!!!!');
    }
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.8
   */
  public notifyFrontendVersionUpdate(): void {
    this._universeGameService.requestWithAutorizationToContext('admin', 'post', 'system/notify-updated-version', 'true')
      .pipe(take(1)).toPromise();
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.9
   */
  public async runHangMissions(): Promise<void> {
    this.runningHangMissions = true;
    await this._universeGameService.requestWithAutorizationToContext('admin', 'post', 'system/run-hang-missions', 'true')
      .pipe(take(1)).toPromise();
    this.runningHangMissions = false;
  }

  /**
   * Data repair: re-grants the special-location unlocks to every current
   * owner of a special-location planet (idempotent). Run once after
   * deploying the fix for the unlocks-never-granted bug.
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 1.0.0
   */
  public async runSpecialLocationUnlockRepair(): Promise<void> {
    this.runningUnlockRepair = true;
    const count = await this._universeGameService
      .requestWithAutorizationToContext('admin', 'post', 'special-location/repair-unlocks', 'true')
      .pipe(take(1)).toPromise();
    this.runningUnlockRepair = false;
    alert(`Repair done, re-evaluated ${count} planet-owner pairs`);
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.16
   */
  public clickSendSystemMessage(): void {
    const content = prompt(`Enter message, empty to discard, note, will NOT be able to delete after sending... for now`);
    this._adminSystemMessageService.saveNew({
      id: null,
      content
    }).toPromise();
  }
}
