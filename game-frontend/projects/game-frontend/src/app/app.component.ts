import { Component, OnInit } from '@angular/core';

import { LoadingService } from '@owge/core';

import { PlanetPojo } from './shared-pojo/planet.pojo';
import { LoginSessionService } from './login-session/login-session.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.less']
})
export class AppComponent implements OnInit {

  public selectedPlanet: PlanetPojo;
  public isInGame: boolean;


  /**
   * Represents a global version of the loading state, any service can force to disable all the interface, by using <i>LoadingService</i>
   *
   * @type {boolean}
   * @memberof AppComponent
   */
  public isLoading: boolean;

  public constructor(private _loginSessionService: LoginSessionService, private _loadingService: LoadingService) {

  }

  public ngOnInit() {
    this._loginSessionService.isInGame.subscribe(isInGame => this.isInGame = isInGame);
    this._loginSessionService.findSelectedPlanet.subscribe(selectedPlanet => this.selectedPlanet = selectedPlanet);
    this._loadingService.observeLoading().subscribe(current => this.isLoading = current);
  }
}
