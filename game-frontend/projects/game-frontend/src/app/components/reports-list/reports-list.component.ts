import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';

import { ModalComponent, ScreenDimensionsService, LoadingService } from '@owge/core';

import { ReportService } from '../../services/report.service';
import { BaseComponent } from '../../base/base.component';
import { MissionReport } from '@owge/universe';
import { UserWithFaction } from '@owge/faction';
import { tap } from 'rxjs/operators';

interface NormalizedMissionReport extends MissionReport {
  normalizedDate?: Date;
}

@Component({
  selector: 'app-reports-list',
  templateUrl: './reports-list.component.html',
  styleUrls: ['./reports-list.component.scss']
})
export class ReportsListComponent extends BaseComponent<UserWithFaction> implements OnInit, OnDestroy {

  public reports: NormalizedMissionReport[];
  public selectedReport: NormalizedMissionReport;
  public isDesktop: boolean;

  @ViewChild('reportDetailsModal', { static: true })
  private _modal: ModalComponent;

  private _page = 1;
  private _identifier: string;
  private _alreadyTaggedAsReaded: Set<number> = new Set();
  private _scrollPosition = 0;

  constructor(
    private _reportService: ReportService,
    private _screenDimensionsService: ScreenDimensionsService,
  ) {
    super();
    this._identifier = _screenDimensionsService.generateIdentifier(this);
  }

  public async ngOnInit() {
    this.requireUser();
    this._screenDimensionsService.hasMinWidth(767, this._identifier).subscribe(val => this.isDesktop = val);
    this._subscriptions.add(this._reportService.findReports<NormalizedMissionReport>().pipe(
      tap(result => result.forEach(report => report.normalizedDate = this._findReportDate(report)))
    ).subscribe(result => {
      this.reports = result;
      const markAsReadReports: MissionReport[] = result
        .filter(current => !current.userReadDate)
        .filter(current => !this._alreadyTaggedAsReaded.has(current.id));
      this._reportService.markAsRead(markAsReadReports).subscribe();
      this._doWithLoading(new Promise(resolve => {
        window.setTimeout(() => {
          window.scrollTo(0, this._scrollPosition);
          resolve();
        }, 250);
      }));
      markAsReadReports.forEach(current => this._alreadyTaggedAsReaded.add(current.id));
    }));
  }

  public ngOnDestroy(): void {
    super.ngOnDestroy();
    this._screenDimensionsService.removeHandler(this._identifier);
  }

  public showReportDetails(report: MissionReport): void {
    this.selectedReport = report;
    this._modal.show();
  }

  public downloadNextPage(): void {
    this._page++;
    this._scrollPosition = this._findCurrentScroll();
    this._doWithLoading(this._reportService.downloadPage(this._page));
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

  private _findCurrentScroll(): number {
    return window.scrollY || window.pageYOffset;
  }
}
