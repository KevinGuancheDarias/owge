import { Injectable } from '@angular/core';
import { Observable, pipe } from 'rxjs';
import { map } from 'rxjs/operators';

import { UniverseGameService } from '@owge/universe';

import { MissionReport } from '../shared/types/mission-report.type';

@Injectable()
export class ReportService {

  public constructor(private _universeGameService: UniverseGameService) {}

  public findReports(page = 1): Observable<MissionReport[]> {
    return this._universeGameService.getWithAuthorizationToUniverse<MissionReport[]>(`report/findMy?page=${page}`)
      .pipe(
          map(result => result.map(current => {
            current.missionDate = new Date(current.missionDate);
            return current;
          })
        )
      );
  }
}
