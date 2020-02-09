import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';

import { ModalComponent, ScreenDimensionsService } from '@owge/core';

import { ReportService } from '../../services/report.service';
import { MissionReport } from '../../shared/types/mission-report.type';
import { BaseComponent } from '../../base/base.component';

interface NormalizedMissionReport extends MissionReport {
  normalizedDate?: Date;
}

@Component({
  selector: 'app-reports-list',
  templateUrl: './reports-list.component.html',
  styleUrls: ['./reports-list.component.scss']
})
export class ReportsListComponent extends BaseComponent implements OnInit, OnDestroy {

  public reports: NormalizedMissionReport[];
  public selectedReport: NormalizedMissionReport;
  public isDesktop: boolean;

  @ViewChild('reportDetailsModal', { static: true })
  private _modal: ModalComponent;

  private _page = 1;
  private _identifier: string;

  constructor(
    private _reportService: ReportService,
    private _screenDimensionsService: ScreenDimensionsService
  ) {
    super();
    this._identifier = _screenDimensionsService.generateIdentifier(this);
  }

  public async ngOnInit() {
    this.requireUser();
    this._screenDimensionsService.hasMinWidth(767, this._identifier).subscribe(val => this.isDesktop = val);
    this.reports = await this._reportService.findReports(this._page).toPromise();
    this.reports.forEach(report => report.normalizedDate = this._findReportDate(report));
  }

  public ngOnDestroy(): void {
    this._screenDimensionsService.removeHandler(this._identifier);
  }

  public showReportDetails(report: MissionReport): void {
    this.selectedReport = report;
    this._modal.show();
  }

  private _findReportDate(report: MissionReport): Date {
    if (report) {
      return report.reportDate
        ? report.reportDate
        : report.missionDate;
    } else {
      return new Date('1970-01-01');
    }
  }
}
