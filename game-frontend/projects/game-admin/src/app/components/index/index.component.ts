import { Component, OnInit, ViewChild } from '@angular/core';
import { LoadingService } from '@owge/core';
import { UniverseGameService } from '@owge/universe';
import { WidgetConfirmationDialogComponent } from '@owge/widgets';
import { take } from 'rxjs/operators';

@Component({
  selector: 'app-index',
  templateUrl: './index.component.html',
  styleUrls: ['./index.component.less']
})
export class IndexComponent {

  public runningHangMissions = false;

  constructor(private _loadingService: LoadingService, private _universeGameService: UniverseGameService) { }


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
}
