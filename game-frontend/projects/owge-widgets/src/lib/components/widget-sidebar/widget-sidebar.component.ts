import { Component, Input } from '@angular/core';

import {  MenuRoute } from '@owge/core';

@Component({
  selector: 'owge-widgets-sidebar',
  templateUrl: './widget-sidebar.component.html',
  styleUrls: ['./widget-sidebar.component.less'],
  providers: []
})
export class WidgetSideBarComponent {
  @Input() public sidebarRoutes: MenuRoute[];

  constructor() {}

}
