import { Component, OnInit, ViewChild } from '@angular/core';

import { ReportService } from '../../services/report.service';
import { MissionReport } from '../../shared/types/mission-report.type';
import { ModalComponent } from '../modal/modal.component';
import { BaseComponent } from '../../base/base.component';
import { ExploreMissionReportJson } from '../../shared/types/explore-mission-report-json.type';

@Component({
  selector: 'app-reports-list',
  templateUrl: './reports-list.component.html',
  styleUrls: ['./reports-list.component.less']
})
export class ReportsListComponent extends BaseComponent implements OnInit {

  public reports: MissionReport[];
  public selectedReport: MissionReport;

  @ViewChild('reportDetailsModal', { static: true })
  private _modal: ModalComponent;

  private _page = 1;

  constructor(
    private _reportService: ReportService
  ) {
    super();
  }

  public async ngOnInit() {
    this.requireUser();
    this.reports = await this._reportService.findReports(this._page).toPromise();
  }

  public showReportDetails(report: MissionReport): void {
    this.selectedReport = report;
    this._modal.show();
  }

  public findReportDate(report: MissionReport): Date {
    if (report) {
      return report.reportDate
        ? report.reportDate
        : report.missionDate;
    } else {
      return new Date('1970-01-01');
    }
  }
}
