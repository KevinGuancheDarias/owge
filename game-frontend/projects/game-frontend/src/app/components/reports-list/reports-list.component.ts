import { Component, OnInit, ViewChild } from '@angular/core';

import { ModalComponent } from '@owge/core';

import { ReportService } from '../../services/report.service';
import { MissionReport } from '../../shared/types/mission-report.type';
import { BaseComponent } from '../../base/base.component';

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
