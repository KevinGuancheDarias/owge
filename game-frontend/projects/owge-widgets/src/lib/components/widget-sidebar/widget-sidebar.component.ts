import { Component, Input, OnInit } from '@angular/core';

import { MenuRoute } from '@owge/core';
import { Router, NavigationEnd, ActivatedRoute } from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'owge-widgets-sidebar',
  templateUrl: './widget-sidebar.component.html',
  styleUrls: ['./widget-sidebar.component.less'],
  providers: []
})
export class WidgetSideBarComponent implements OnInit {
  @Input() public sidebarRoutes: MenuRoute[];

  public selectedRoute: MenuRoute;
  public computedLength = '100%';

  constructor(private _router: Router) {

  }

  public ngOnInit(): void {
    this.selectedRoute = this.sidebarRoutes.find(current => this._router.url.startsWith(current.path));
    this._router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe((route: NavigationEnd) => {
      this.selectedRoute = this.sidebarRoutes.find(current => route.url.startsWith(current.path));
      this._calculateSvgLength();
    });
    this._calculateSvgLength();
  }

  private _calculateSvgLength(): void {
    if (this.selectedRoute) {
      const length: number = this.selectedRoute.text.length * 8;
      this.computedLength = ((length > 100) ? 100 : length) + '%';
    }
  }
}
