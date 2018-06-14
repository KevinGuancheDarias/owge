
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { ROUTES } from './../config/config.pojo';
import { LoginService } from './login.service';
import { LoginSessionService } from '../login-session/login-session.service';
import { WebsocketService } from '../service/websocket.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.less']
})
export class LoginComponent implements OnInit {

  public email: string;
  public password: string;

  constructor(
    private _loginService: LoginService,
    private _loginSessionService: LoginSessionService,
    private _websocketService: WebsocketService,
    private _router: Router
  ) { }

  ngOnInit() {
    this._loginSessionService.findLoggedInUserData().filter(status => !!status).subscribe(() => {
      this._router.navigate([ROUTES.GAME_INDEX]);
    });
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
