import { Injectable } from '@angular/core';
import { Observable, pipe } from 'rxjs';
import { map } from 'rxjs/operators';
import { MissionReport } from '../shared/types/mission-report.type';
import { CoreGameService } from '../modules/core/services/core-game.service';

@Injectable()
export class ReportService {

  public constructor(private _coreGameService: CoreGameService) {}

  public findReports(page = 1): Observable<MissionReport[]> {
    return this._coreGameService.getWithAuthorizationToUniverse<MissionReport[]>(`report/findMy?page=${page}`)
      .pipe(
          map(result => result.map(current => {
            current.missionDate = new Date(current.missionDate);
            return current;
          })
        )
      );
  }
}
