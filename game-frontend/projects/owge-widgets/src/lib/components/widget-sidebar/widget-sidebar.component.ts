import {
  Component, Input, OnInit, OnChanges, ViewChild, ElementRef,
  AfterContentInit, TemplateRef, ContentChildren, QueryList
} from '@angular/core';

import { MenuRoute, ScreenDimensionsService, ContentTransclusionUtil, OwgeContentDirective } from '@owge/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter, takeUntil } from 'rxjs/operators';
import { fromEvent, Subject, combineLatest } from 'rxjs';

/**
 * Creates a displayable sidebar
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Component({
  selector: 'owge-widgets-sidebar',
  templateUrl: './widget-sidebar.component.html',
  styleUrls: ['./widget-sidebar.component.scss']
})
export class WidgetSideBarComponent implements OnInit, OnChanges, AfterContentInit {
  private static readonly _ICON_WIDTH = 40;

  @Input() public sidebarRoutes: MenuRoute[];

  public selectedRoute: MenuRoute;
  public computedLength = '100%';
  public largestRouteName: MenuRoute;
  public computedTitleWidth: number;
  public isGreaterThanViewportWidth: boolean;
  public isDesktop: boolean;
  public extraButtonContent: TemplateRef<any>;

  @ViewChild('largestRouteEl', { static: false }) private _largestRouteEl: ElementRef;
  @ContentChildren(OwgeContentDirective) private _templatesList: QueryList<OwgeContentDirective>;

  private _changedSubject: Subject<void>;
  private _sdsWidthId: string;
  private _sdsHeightId: string;

  constructor(private _router: Router, private _screenDimensionsService: ScreenDimensionsService) {

  }

  public ngOnInit(): void {
    this._calculateComputedTitleWidth();
    this._sdsWidthId = this._screenDimensionsService.generateIdentifier(this.constructor.name);
    this._sdsHeightId = this._screenDimensionsService.generateIdentifier(this.constructor.name);
    combineLatest([
      this._screenDimensionsService.hasMinWidth(768, this._sdsWidthId),
      this._screenDimensionsService.hasMinHeight(760, this._sdsHeightId)
    ]).subscribe(results => {
      this.isDesktop = results.every(current => current);
      console.log('Fired that shit', this.isDesktop);
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
