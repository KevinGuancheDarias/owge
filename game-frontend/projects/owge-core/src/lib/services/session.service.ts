import { Injectable } from '@angular/core';
import { OwgeUserModule } from '../owge-user.module';

import { Router, ActivatedRouteSnapshot, RouterStateSnapshot, CanActivate } from '@angular/router';
import { Observable } from 'rxjs';

import { TokenPojo } from '../pojos/token.pojo';
import { AccountConfig } from '../pojos/account-config.pojo';
import { ProgrammingError } from '../errors/programming.error';
import { UserStorage } from '../storages/user.storage';
import { User } from '../types/user.type';

/**
 * Modern implementation of session control (replacement for good old' SessionService)
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable({ providedIn: OwgeUserModule})
export class SessionService implements CanActivate {
    public static readonly LOGIN_ROUTE = '/login';
    public static readonly LOCAL_STORAGE_TOKEN_PARAM = 'owge_authentication';

    public constructor(private _router: Router, private _accountConfig: AccountConfig, private _userStore: UserStorage<User>) {
    }

    public canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
        const loadingRoute = route.url[0].path;
        if (this.hasLoginDomain()) {
          if ((this.isLoginDomain() && this.isLoggedIn() && !this._isLoginRoute(loadingRoute))
            || (!this.isLoginDomain() && !this.isLoggedIn())) {
            this.logout();
            window.location.href = `//${this._accountConfig.loginDomain}`;
            return false;
          } else if ((!this.isLoginDomain() && this.isLoggedIn())
            || (this.isLoginDomain() && this.isLoggedIn() && this._isLoginRoute(loadingRoute))) {
            return true;
          } else if (this.isLoginDomain() && !this._isLoginRoute(loadingRoute)) {
            this.logout();
          } else {
            throw new ProgrammingError('Situation not expected');
          }
        } else {
          this._redirectIfNotLoggedIn();
        }
        return this.isLoggedIn();
    }

    public hasLoginDomain(): boolean {
        return !!this._accountConfig.loginDomain;
    }

    public isLoginDomain(): boolean {
        if (!this.hasLoginDomain()) {
          throw new ProgrammingError('Can NOT invoke this method when the env has no login route');
        }
        return this._accountConfig.loginDomain === window.document.domain;
    }

    public isLoggedIn(): boolean {
        this._removeSessionStorageDataIfSessionExpired();
        return !!this._findTokenIfNotExpired();
    }

    public getParsedToken(): TokenPojo {
        const jwtToken: string = sessionStorage.getItem(SessionService.LOCAL_STORAGE_TOKEN_PARAM);
        return this._parseToken(jwtToken);
    }

    public getRawToken() {
        return sessionStorage.getItem(SessionService.LOCAL_STORAGE_TOKEN_PARAM);
    }

    public logout(): any {
        this._clearSessionData();
        this._redirectIfNotLoggedIn();
    }

    /**
   * @param  token - RAW encoded token
   * @return the encoded token
   */
  public setTokenPojo(token): void {
    this._userStore.currentToken.next(token);
    sessionStorage.setItem(SessionService.LOCAL_STORAGE_TOKEN_PARAM, token);
  }

   /**
    * Will return the parsed Token
    *
    * @param  jwtToken - The jwt token
    * @return  the encoded token
    * @author Kevin Guanche Darias
    */
    private _parseToken(jwtToken: string): TokenPojo {
        if (jwtToken) {
            const retVal: TokenPojo = JSON.parse(atob(jwtToken.split('.')[1]));
            retVal.exp *= 1000;
            return retVal;
        } else {
            return null;
        }
    }

    private _redirectTo(route: string): void {
        this._router.navigate([route]);
    }

    private _redirectIfNotLoggedIn(): void {
        if (!this.isLoggedIn()) {
            console.log('SessionService: Redirecting to ' + SessionService.LOGIN_ROUTE);
            this._redirectTo(SessionService.LOGIN_ROUTE);
        }
    }

    private _removeSessionStorageDataIfSessionExpired() {
        if (!this._findTokenIfNotExpired()) {
            this._clearSessionData();
        }
    }

    private _findTokenIfNotExpired(): TokenPojo {
        let retVal: TokenPojo;
        if (this.getRawToken()) {
          const sessionToken: TokenPojo = this.getParsedToken();
          if (sessionToken) {
            if (!TokenPojo.isExpired(sessionToken)) {
              retVal = sessionToken;
            } else {
              this._clearSessionData();
            }
          }
        }
        return retVal;
    }

    private _clearSessionData() {
        sessionStorage.removeItem(SessionService.LOCAL_STORAGE_TOKEN_PARAM);
    }

    private _isLoginRoute(route: string): boolean {
        return route === 'universe-selection' || route === 'faction-selection' || route === 'synchronice-credentials';
    }
}
