import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  UniverseGameService, MissionReport, MissionReportResponse, ReportStore,
  UniverseCacheManagerService, WsEventCacheService
} from '@owge/universe';
import { AbstractWebsocketApplicationHandler, StorageOfflineHelper } from '@owge/core';
import { map, take, tap } from 'rxjs/operators';

@Injectable()
export class ReportService extends AbstractWebsocketApplicationHandler {

  private _currentReports: MissionReport[] = [];
  private _currentCounts: MissionReportResponse;

  private _alreadyDownloadedReports: Set<number> = new Set();
  private _reportStore: ReportStore = new ReportStore;
  private _offlineChangeCache: StorageOfflineHelper<MissionReportResponse>;
  private _offlineCountChangeCache: StorageOfflineHelper<MissionReportResponse>;

  public constructor(
    private _universeGameService: UniverseGameService,
    private _wsEventCacheService: WsEventCacheService,
    private _universeCacheManagerService: UniverseCacheManagerService
  ) {
    super();
    this._eventsMap = {
      mission_report_change: '_onChange',
      mission_report_count_change: '_onCountChange'
    };
  }

  public async createStores(): Promise<void> {
    this._offlineChangeCache = this._universeCacheManagerService.getStore('reports.change');
    this._offlineCountChangeCache = this._universeCacheManagerService.getStore('reports.count_change');
  }

  public findReports<T extends MissionReport = MissionReport>(): Observable<T[]> {
    return <any>this._reportStore.reports.asObservable();
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   */
  public async workaroundSync(): Promise<void> {
    this._onChange(
      await this._wsEventCacheService.findFromCacheOrRun(
        'mission_report_change',
        this._offlineChangeCache,
        async () => await this._doDownloadPage().toPromise()
      )
    );
  }

  public async workaroundInitialOffline(): Promise<void> {
    await this._offlineChangeCache.doIfNotNull(content => this._onChange(content));
    await this._offlineCountChangeCache.doIfNotNull(content => this._onCountChange(content));
  }

  /**
   * Downlods a page, doesn't return, as we trigger a change in the subject
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param page
   */
  public async downloadPage(page: number): Promise<void> {
    this._handleReportsDownload(await this._doDownloadPage(page).pipe(
      map(result => result.reports),
      take(1)
    ).toPromise());
    this._reportStore.reports.next(this._currentReports);
  }

  /**
   * Marks the reports as read
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param reports
   * @returns
   */
  public async markAsRead(reports: MissionReport[]): Promise<void> {
    const reportsIds: number[] = reports.map(current => current.id);
    await this._universeGameService.requestWithAutorizationToContext('game', 'post', 'report/mark-as-read', reportsIds).toPromise();
    reportsIds.forEach(current => {
      const reportWithThatID = this._currentReports.find(report => report.id === current);
      if (reportWithThatID) {
        reportWithThatID.userReadDate = new Date();
      }
    });
    this._reportStore.reports.next(this._currentReports);
    await this._offlineChangeCache.save({ ...this._currentCounts, reports: this._currentReports });
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returns
   */
  public findUserUnreadCount(): Observable<number> {
    return this._reportStore.userUnread.asObservable();
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returns
   */
  public findEnemyUnreadCount(): Observable<number> {
    return this._reportStore.enemyUnread.asObservable();
  }

  protected _doDownloadPage(page = 1): Observable<MissionReportResponse> {
    return this._universeGameService.requestWithAutorizationToContext<MissionReportResponse>('game', 'get', `report/findMy?page=${page}`);
  }

  protected async _onChange(content: MissionReportResponse): Promise<void> {
    if (!this._isCachePanic(content)) {
      this._onCountChange(content);
      this._handleReportsDownload(content.reports);
      this._reportStore.reports.next(this._currentReports);
      await this._offlineChangeCache.save(content);
    }
  }

  protected async _onCountChange(content: MissionReportResponse): Promise<void> {
    if (!this._isCachePanic(content)) {
      this._currentCounts = content;
      this._reportStore.userUnread.next(content.userUnread);
      this._reportStore.enemyUnread.next(content.enemyUnread);
      await this._offlineCountChangeCache.save(content);
    }
  }

  private _handleReportsDownload(reports: MissionReport[]): void {
    reports.filter(current => !this._alreadyDownloadedReports.has(current.id))
      .map(current => {
        this._currentReports.push(current);
        this._currentReports = this._currentReports.sort((a, b) => a.id > b.id ? -1 : 1);
        this._alreadyDownloadedReports.add(current.id);
        current.missionDate = new Date(current.missionDate);
        return current;
      });
  }
}
