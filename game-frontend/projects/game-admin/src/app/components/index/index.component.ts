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
}
