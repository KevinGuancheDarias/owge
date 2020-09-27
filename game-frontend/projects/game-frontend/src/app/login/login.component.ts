import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { filter, take } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';

import { ROUTES, LoginService, SessionService } from '@owge/core';
import { UniverseGameService } from '@owge/universe';

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
  public kgdwGamesUrl = '';

  constructor(
    private _translateService: TranslateService,
    private _loginService: LoginService,
    private _sessionService: SessionService,
    private _router: Router,
    private _universeGameService: UniverseGameService
  ) { }

  ngOnInit() {
    this._defineKgdwLang(this._translateService.currentLang ? this._translateService.currentLang : this._translateService.defaultLang);
    this._translateService.onLangChange.subscribe(lang => this._defineKgdwLang(lang));
    if (this._sessionService.hasLoginDomain() && this._sessionService.isLoginDomain() && this._sessionService.isLoggedIn()) {
      this._sessionService.logout();
    } else if (this._sessionService.hasLoginDomain() && !this._sessionService.isLoginDomain()) {
      if (!this._sessionService.isLoggedIn()) {
        window.location.href = `//${environment.loginDomain}`;
      } else {
        this._universeGameService.findLoggedInUserData().pipe(filter(status => !!status), take(1)).subscribe(() => {
          this._router.navigate([ROUTES.GAME_INDEX]);
        });
      }
    } else if (this._sessionService.isLoggedIn()) {
      this._universeGameService.findLoggedInUserData().pipe(filter(status => !!status)).subscribe(() => {
        this._router.navigate([ROUTES.GAME_INDEX]);
      });
    }
  }

  onLoginFormSubmit() {
    this._loginService.login(this.email, this.password)
      .subscribe(
        token => this.onLoginSuccess(token)
      );
  }


  /**
   * Call when login is ok
   *
   * @param {string} token Token from backend
   * @author Kevin Guanche Darias
   */
  private onLoginSuccess(token: string): void {
    this._router.navigate(['/universe-selection']);
  }

  private _defineKgdwLang(lang: string): void {
    let pathAlias;
    switch (lang) {
      case 'es':
        pathAlias = 'juegos';
        break;
      case 'en':
      default:
        pathAlias = 'games';
        break;
    }
    this.kgdwGamesUrl = `${this.accountUrl}${lang}/${pathAlias}`;
  }
}
