import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import {filter} from 'rxjs/operators';

import { ROUTES, LoginService, SessionService } from '@owge/core';
import { UniverseGameService } from '@owge/universe';

import { WebsocketService } from '../service/websocket.service';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.less']
})
export class LoginComponent implements OnInit {

  public email: string;
  public password: string;
  public accountUrl: string = environment.accountUrl;

  constructor(
    private _loginService: LoginService,
    private _sessionService: SessionService,
    private _websocketService: WebsocketService,
    private _router: Router,
    private _universeGameService: UniverseGameService
  ) { }

  ngOnInit() {
    if (this._sessionService.hasLoginDomain() && this._sessionService.isLoginDomain() && this._sessionService.isLoggedIn()) {
      this._sessionService.logout();
    } else if (this._sessionService.hasLoginDomain() && !this._sessionService.isLoginDomain()) {
      if (!this._sessionService.isLoggedIn()) {
        window.location.href = `//${environment.loginDomain}`;
      } else {
        this._universeGameService.findLoggedInUserData().pipe(filter(status => !!status)).subscribe(() => {
          this._router.navigate([ROUTES.GAME_INDEX]);
        });
      }
    } else {
      this._universeGameService.findLoggedInUserData().pipe(filter(status => !!status)).subscribe(() => {
        this._router.navigate([ROUTES.GAME_INDEX]);
      });
    }
  }

  onLoginFormSubmit() {
    this._loginService.login(this.email, this.password)
      .subscribe(
        token => this.onLoginSuccess(token),
        error => alert(error)
      );
  }


  /**
   * Call when login is ok
   *
   * @param {string} token Token from backend
   * @author Kevin Guanche Darias
   */
  private onLoginSuccess(token: string): void {
    this._websocketService.authenticate(token);
    this._router.navigate(['/universe-selection']);
  }
}
