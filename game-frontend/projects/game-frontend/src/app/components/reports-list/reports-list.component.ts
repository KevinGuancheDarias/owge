import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';

import { ModalComponent, ScreenDimensionsService, ObservableSubscriptionsHelper } from '@owge/core';

import { ReportService } from '../../services/report.service';
import { BaseComponent } from '../../base/base.component';
import { MissionReport, WebsocketService } from '@owge/universe';
import { UserWithFaction } from '@owge/faction';
import { tap, filter } from 'rxjs/operators';

interface NormalizedMissionReport extends MissionReport {
  normalizedDate?: Date;
}

@Component({
  selector: 'app-reports-list',
  templateUrl: './reports-list.component.html',
  styleUrls: ['./reports-list.component.scss']
})
export class ReportsListComponent extends BaseComponent<UserWithFaction> implements OnInit, OnDestroy {

  @ViewChild('reportDetailsModal', { static: true })
  private _modal: ModalComponent;

  public reports: NormalizedMissionReport[];
  public selectedReport: NormalizedMissionReport;
  public isDesktop: boolean;

  private _page = 1;
  private _identifier: string;
  private _alreadyTaggedAsReaded: Set<number> = new Set();
  private _scrollPosition = 0;
  private _markAsReadSubscriptions: ObservableSubscriptionsHelper = new ObservableSubscriptionsHelper;

  constructor(
    private _reportService: ReportService,
    private _screenDimensionsService: ScreenDimensionsService,
    private _websocketService: WebsocketService
  ) {
    super();
    this._identifier = _screenDimensionsService.generateIdentifier(this);
  }

  async ngOnInit() {
    this.requireUser();
    this._reportService.setDoSplit(false);
    this._screenDimensionsService.hasMinWidth(767, this._identifier).subscribe(val => this.isDesktop = val);
    this._subscriptions.add(this._reportService.findReports<NormalizedMissionReport>().pipe(
      tap(result => result.forEach(report => report.normalizedDate = this._findReportDate(report)))
    ).subscribe(result => {
      this.reports = result;
      if (result.some(currentReport => !currentReport.userReadDate)) {
        this._markAsReadSubscriptions.add(this._websocketService.isConnected.pipe(filter(connected => connected)).subscribe(() => {
          const markAsReadReports: MissionReport[] = result
            .filter(current => !current.userReadDate)
            .filter(current => !this._alreadyTaggedAsReaded.has(current.id));
          this._doWithLoading(
            this._reportService.markAsRead(markAsReadReports),
            new Promise<void>(resolve => {
              window.setTimeout(() => {
                window.scrollTo(0, this._scrollPosition);
                resolve();
              }, 250);
            }));
          markAsReadReports.forEach(current => this._alreadyTaggedAsReaded.add(current.id));
        }));
      }
    }));
  }

  public ngOnDestroy(): void {
    super.ngOnDestroy();
    this._screenDimensionsService.removeHandler(this._identifier);
    this._markAsReadSubscriptions.unsubscribeAll();
    this._reportService.setDoSplit(true);
  }

  public markAllAsRead(): void {
    this._loadingService.addPromise(this._reportService.markAllAsRead());
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
