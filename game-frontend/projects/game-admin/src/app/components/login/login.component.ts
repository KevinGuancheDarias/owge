import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';

import { ROUTES } from '@owge/core';
import { Universe, UniverseService, UniverseStorage } from '@owge/universe';
import { AdminLoginService } from '../../services/admin-login.service';

/**
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.less']
})
export class LoginComponent implements OnInit {
  public email: string;
  public password: string;
  public selectedUniverse: Universe;
  public universes: Universe[];

  constructor(
    private _adminLoginService: AdminLoginService,
    private _router: Router,
    private _universeService: UniverseService,
    private _universeStore: UniverseStorage
    ) { }

  public ngOnInit(): void {
    this._universeService.findOfficials().subscribe(universes => this.universes = universes );
    this._universeStore.currentUniverse.subscribe(universe => this.selectedUniverse = universe);
  }

  public onUniverseSelected(universe: Universe): void {
    if (universe) {
      this._universeService.setSelectedUniverse(universe);
    }
  }

  /**
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @memberof LoginComponent
   */
  public doLogin(): void {
    this._adminLoginService.login(this.email, this.password).pipe(catchError(err => {
      alert(`Unexpected error ${err}`);
      return err;
    })). subscribe(() => {
      this._router.navigate([ROUTES.GAME_INDEX]);
    });
  }

}
