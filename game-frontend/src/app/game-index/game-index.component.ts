import { Component, OnInit } from '@angular/core';

import { BaseComponent } from '../base/base.component';

@Component({
  selector: 'app-game-index',
  templateUrl: './game-index.component.html',
  styleUrls: ['./game-index.component.less']
})
export class GameIndexComponent extends BaseComponent implements OnInit {

  public constructor() {
    super();
  }

  /**
   * Notice
   */
  public ngOnInit() {
    this.requireUser();
  }
}
