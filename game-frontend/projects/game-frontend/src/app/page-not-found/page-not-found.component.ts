import { Router } from '@angular/router';
import { Component, OnInit } from '@angular/core';

import { ROUTES } from '@owge/core';

import { BaseComponent } from '../base/base.component';


/**
 *
 * @deprecated As of 0.8.0 use OwgeCoreModule/PageNotFoundModule
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.3.0
 * @export
 */
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
