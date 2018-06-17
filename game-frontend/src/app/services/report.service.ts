import { Injectable } from '@angular/core';
import { GameBaseService } from '../service/game-base.service';
import { Observable } from 'rxjs/Observable';
import { MissionReport } from '../shared/types/mission-report.type';
import { ExploreMissionReportJson } from '../shared/types/explore-mission-report-json.type';
import { MissionReportJson } from '../shared/types/mission-report-json.type';

@Injectable()
export class ReportService extends GameBaseService {

  public findReports(page = 1): Observable<MissionReport[]> {
    return this.doGetWithAuthorizationToGame<MissionReport[]>(`report/findMy?page=${page}`).map(result => result.map(current => {
      current.missionDate = new Date(current.missionDate);
      return current;
    }));
  }
}
