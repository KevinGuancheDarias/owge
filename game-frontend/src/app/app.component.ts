import { PlanetPojo } from './shared-pojo/planet.pojo';
import { LoginSessionService } from './login-session/login-session.service';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.less']
})
export class AppComponent implements OnInit {

  public selectedPlanet: PlanetPojo;
  public isInGame: boolean;
  public someDate: Date;

  public constructor(private _loginSessionService: LoginSessionService) {

  }

  public ngOnInit() {
    this._loginSessionService.isInGame.subscribe(isInGame => this.isInGame = isInGame);
    this._loginSessionService.findSelectedPlanet.subscribe(selectedPlanet => this.selectedPlanet = selectedPlanet);
    let now = new Date();
    this.someDate = new Date(now.getTime() + 100000);
  }
}
