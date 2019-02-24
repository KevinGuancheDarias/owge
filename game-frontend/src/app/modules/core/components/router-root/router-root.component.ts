import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { RouterData } from '../../types/router-data.type';
import { ProgrammingError } from '../../../../../error/programming.error';
import { MenuRoute } from '../../types/menu-route.type';
import { getCurrentDebugContext } from '@angular/core/src/view/services';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @class RouterRootComponent
 * @implements {OnInit}
 */
@Component({
  selector: 'app-router-root',
  templateUrl: './router-root.component.html',
  styleUrls: ['./router-root.component.less']
})
export class RouterRootComponent implements OnInit {

  public routerData: RouterData;

  public constructor(private _route: ActivatedRoute) {

  }

  public ngOnInit() {
    this._route.data.subscribe(data => {
      if (!data.sectionTitle) {
        throw new ProgrammingError(`Invalid input for ${this.constructor.name}`);
      }
      this.routerData = <any>data;
      if (this.routerData.routes) {
        this.routerData.routes.forEach(current => {
          if (typeof current.ngIf === 'function') {
            current.ngIf().then(result => current.isNgIfOk = result);
          } else {
            current.isNgIfOk = true;
          }
        });
      }
    });
  }

}
