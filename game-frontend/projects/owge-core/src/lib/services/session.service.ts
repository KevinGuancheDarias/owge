import { Injectable } from '@angular/core';
import { OwgeUserModule } from '../owge-user.module';

import { Router, ActivatedRouteSnapshot, RouterStateSnapshot, CanActivate } from '@angular/router';
import { Observable } from 'rxjs';

import { TokenPojo } from '../pojos/token.pojo';
import { AccountConfig } from '../pojos/account-config.pojo';
import { ProgrammingError } from '../errors/programming.error';
import { UserStorage } from '../storages/user.storage';
import { User } from '../types/user.type';
import { JwtTokenUtil } from '../utils/jwt-token.util';

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

    /**
     * Inits the stores
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @memberof SessionService
     */
    public initStore(): void {
      const token = this._findTokenIfNotExpired();
      if (token) {
        this._userStore.currentToken.next(this.getRawToken());
        this._userStore.currentUser.next(token.data);
      }
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
        return JwtTokenUtil.parseToken(this.getRawToken());
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

    private _redirectTo(route: string): void {
        this._router.navigate([route]);
    }

    private _redirectIfNotLoggedIn(): void {
        if (!this.isLoggedIn()) {
            console.log('SessionService: Redirecting to ' + SessionService.LOGIN_ROUTE);
            this._redirectTo(SessionService.LOGIN_ROUTE);
        }
    }

    /**
     *
     * @todo Investigate if this method makes sense... look at _findTokenIfNotExpired()
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private _removeSessionStorageDataIfSessionExpired() {
        if (!this._findTokenIfNotExpired()) {
            this._clearSessionData();
        }
    }

    private _findTokenIfNotExpired(): TokenPojo {
        return JwtTokenUtil.findTokenIfNotExpired(this.getRawToken(), () => this._clearSessionData());
    }

    private _clearSessionData() {
        sessionStorage.removeItem(SessionService.LOCAL_STORAGE_TOKEN_PARAM);
    }

    private _isLoginRoute(route: string): boolean {
        return route === 'universe-selection' || route === 'faction-selection' || route === 'synchronice-credentials';
    }
}
