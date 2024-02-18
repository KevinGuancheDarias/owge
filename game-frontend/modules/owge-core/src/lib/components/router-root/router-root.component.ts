import { Component, OnInit, Injector } from '@angular/core';
import { ActivatedRoute, Router, NavigationEnd } from '@angular/router';
import { RouterData } from '@owge/core';
import { ProgrammingError } from '../../errors/programming.error';
import { filter } from 'rxjs/operators';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
@Component({
  selector: 'owge-core-router-root',
  templateUrl: './router-root.component.html',
  styleUrls: ['./router-root.component.less']
})
export class RouterRootComponent implements OnInit {

  public routerData: RouterData;

  public constructor(private _route: ActivatedRoute, private _router: Router, private _injector: Injector) {

  }

  public ngOnInit() {
    this._route.data.subscribe(data => {
      if (!data.sectionTitle) {
        throw new ProgrammingError(`Invalid input for ${this.constructor.name}`);
      }
      this.routerData = <any>data;
      if (this.routerData.default) {
        if (!this._route.snapshot.children.length) {
          this._router.navigate([this._route.snapshot.routeConfig.path, this.routerData.default]);
        }
      }
      this._handleNgIfs();
    });
    this._router.events.pipe(
      filter(current => current instanceof NavigationEnd)
    ).subscribe(event => this._handleNgIfs());
  }

  private _handleNgIfs(): void {
    if (this.routerData.routes) {
      this.routerData.routes.forEach(current => {
        if (typeof current.ngIf === 'function') {
          current.ngIf(this._injector).then(result => current.isNgIfOk = result);
        } else {
          current.isNgIfOk = true;
        }
      });
    }
  }

}
