import { Router } from '@angular/router';
import { ROUTES } from '../config/config.pojo';
import { Component, OnInit } from '@angular/core';

import { BaseComponent } from '../base/base.component';

@Component({
  selector: 'app-page-not-found',
  templateUrl: './page-not-found.component.html',
  styleUrls: ['./page-not-found.component.less']
})
export class PageNotFoundComponent extends BaseComponent implements OnInit {

  constructor(private _router: Router) {
    super();
  }

  ngOnInit() {
    if (this.loginSessionService.isLoggedIn()) {
      this._router.navigate([ROUTES.GAME_INDEX]);
    } else {
      this._router.navigate([ROUTES.LOGIN]);
    }
  }

}
