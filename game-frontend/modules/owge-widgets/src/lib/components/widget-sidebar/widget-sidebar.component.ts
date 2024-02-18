import {
  AfterContentInit, Component,
  ContentChildren, ElementRef, Input, OnChanges, OnInit,
  QueryList, TemplateRef, ViewChild
} from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { ContentTransclusionUtil, OwgeContentDirective, ScreenDimensionsService, SessionStore } from '@owge/core';
import { MenuRoute } from '@owge/core';
import { combineLatest, fromEvent, Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';


/**
 * Creates a displayable sidebar
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Component({
  selector: 'owge-widgets-sidebar',
  templateUrl: './widget-sidebar.component.html'
})
export class WidgetSideBarComponent implements OnInit, OnChanges, AfterContentInit {
  private static readonly _ICON_WIDTH = 40;

  @Input() public sidebarRoutes: MenuRoute[];
  @ViewChild('largestRouteEl') private _largestRouteEl: ElementRef;
  @ContentChildren(OwgeContentDirective) private _templatesList: QueryList<OwgeContentDirective>;

  public selectedRoute: MenuRoute;
  public computedLength = '100%';
  public largestRouteName: MenuRoute;
  public computedTitleWidth: number;
  public isGreaterThanViewportWidth: boolean;
  public isDesktop: boolean;
  public extraButtonContent: TemplateRef<any>;
  public isConnected = true;

  private _changedSubject: Subject<void>;
  private _sdsWidthId: string;
  private _sdsHeightId: string;
  private _disconnectedMessage: string;

  constructor(
    private _router: Router,
    private _activeRoute: ActivatedRoute,
    private _screenDimensionsService: ScreenDimensionsService,
    private _sessionStore: SessionStore,
    translateService: TranslateService
  ) {
    translateService.get('APP.NOT_CONNECTED_CLICK_ERROR').subscribe(val => this._disconnectedMessage = val);
  }

  public ngOnInit(): void {
    this._calculateComputedTitleWidth();
    setTimeout(() => this._sessionStore.isConnected.subscribe(val => {
      this.isConnected = val;
      setTimeout(() => this._router.navigate(this._activeRoute.snapshot.url), 1000);
    }), 2000);
    this._sdsWidthId = this._screenDimensionsService.generateIdentifier(this.constructor.name);
    this._sdsHeightId = this._screenDimensionsService.generateIdentifier(this.constructor.name);
    combineLatest([
      this._screenDimensionsService.hasMinWidth(768, this._sdsWidthId),
      this._screenDimensionsService.hasMinHeight(760, this._sdsHeightId)
    ]).subscribe(results => {
      this.isDesktop = results.every(current => current);
    });
    this.selectedRoute = this.sidebarRoutes.find(current => this._router.url.startsWith(current.path));
    this._router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe((route: NavigationEnd) => {
      this.selectedRoute = this.sidebarRoutes.find(current => route.url.startsWith(current.path));
      this._calculateSvgLength();
    });
    this._calculateSvgLength();
  }

  public ngOnChanges(): void {
    this._calculateComputedTitleWidth();
  }

  public ngAfterContentInit(): void {
    this.extraButtonContent = ContentTransclusionUtil.findInList(this._templatesList, 'extra-button-content');
  }

  public checkRequiredConnection(e: Event, route: MenuRoute): void {
    if (route.isConnectionRequired && !this.isConnected) {
      alert(this._disconnectedMessage);
      e.preventDefault();
      e.stopPropagation();
    }
  }

  private _calculateSvgLength(): void {
    if (this.selectedRoute) {
      const length: number = this.selectedRoute.text.length * 8;
      this.computedLength = ((length > 100) ? 100 : length) + '%';
    }
  }

  private _calculateComputedTitleWidth(): void {
    const largestSize: number = Math.max(...this.sidebarRoutes.map(current => current.text.length));
    this.largestRouteName = this.sidebarRoutes.find(current => current.text.length === largestSize);
    if (this._changedSubject) {
      this._changedSubject.next();
    }
    this._changedSubject = new Subject;
    setTimeout(() => {
      if (this._largestRouteEl) {
        this.computedTitleWidth = this._largestRouteEl.nativeElement.clientWidth;
        const widthPlusIcon = this.computedTitleWidth + WidgetSideBarComponent._ICON_WIDTH;
        this.isGreaterThanViewportWidth = widthPlusIcon > window.innerWidth;
        fromEvent(window, 'resize')
          .pipe(takeUntil(this._changedSubject))
          .subscribe(() => {
            this.isGreaterThanViewportWidth = widthPlusIcon > window.innerWidth;
          });
      }
    }, 100);
  }
}
