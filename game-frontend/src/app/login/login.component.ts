import { ROUTES } from './../config/config.pojo';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { LoginService } from './login.service';
import { LoginSessionService } from '../login-session/login-session.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.less']
})
export class LoginComponent implements OnInit {

  public email: string;
  public password: string;

  public loginCredentials: Object;
  constructor(
    private loginService: LoginService,
    private loginSessionService: LoginSessionService,
    private _router: Router
  ) { }

  public ngOnInit() {
    this.loginSessionService.findLoggedInUserData().filter(status => !!status).subscribe(() => {
      this._router.navigate([ROUTES.GAME_INDEX]);
    });
  }

  onLoginFormSubmit() {
    this.loginService.login(this.email, this.password)
      .subscribe(
        loginData => this.onLoginSuccess(loginData),
        error => alert(error)
      );
  }


  /**
   * Call when login is ok
   * @param data - Data sent by the server, which is unrequired, but who knows!
   * @author Kevin Guanche Darias
   */
  private onLoginSuccess(data: any): void {
    this._router.navigate(['/universe-selection']);
  }
}
