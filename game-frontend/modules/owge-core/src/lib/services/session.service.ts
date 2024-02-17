import { EventEmitter, Injectable } from '@angular/core';
import { OwgeUserModule } from '../owge-user.module';

import { Router, ActivatedRouteSnapshot, RouterStateSnapshot, CanActivate } from '@angular/router';
import { Observable } from 'rxjs';

import { TokenPojo } from '../pojos/token.pojo';
import { OwgeCoreConfig } from '../pojos/owge-core-config';
import { ProgrammingError } from '../errors/programming.error';
import { JwtTokenUtil } from '../utils/jwt-token.util';
import { SessionStore } from '../store/session.store';
import { AbstractWebsocketApplicationHandler } from '../interfaces/abstract-websocket-application-handler';
import { ToastrService } from './toastr.service';

/**
 * Modern implementation of session control (replacement for good old' SessionService)
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable({ providedIn: OwgeUserModule })
export class SessionService extends AbstractWebsocketApplicationHandler implements CanActivate {
  public static readonly LOGIN_ROUTE = '/login';
  public static readonly LOCAL_STORAGE_TOKEN_PARAM = 'owge_authentication';

  /**
   *
   * @since 0.9.6
   */
  public get onLogout(): Observable<void> {
    return this._logoutEmitter.asObservable();
  }

  private _logoutEmitter: EventEmitter<void> = new EventEmitter();

  public constructor(
    private _router: Router,
    private _accountConfig: OwgeCoreConfig,
    private sessionStore: SessionStore,
    private toastrService: ToastrService,
  ) {
    super();
    this._eventsMap = {
      // eslint-disable-next-line @typescript-eslint/naming-convention
      account_deleted: 'onAccountDelete'
    };
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
      this.sessionStore.next('currentToken', this.getRawToken());
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

  public getRawToken() {
    return sessionStorage.getItem(SessionService.LOCAL_STORAGE_TOKEN_PARAM);
  }

  public logout(): void {
    this._clearSessionData();
    this._redirectIfNotLoggedIn();
    this._logoutEmitter.emit();
  }

  /**
   * @param  token - RAW encoded token
   * @return the encoded token
   */
  public setTokenPojo(token): void {
    this.sessionStore.next('currentToken', token);
    sessionStorage.setItem(SessionService.LOCAL_STORAGE_TOKEN_PARAM, token);
  }

  protected onAccountDelete(): void {
    this.toastrService.warn('USER.ACCOUNT_DELETED');
    window.setTimeout(() => this.logout(), 3000);
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
    this.sessionStore.next('currentToken', null);
  }

  private _isLoginRoute(route: string): boolean {
    return route === 'universe-selection' || route === 'faction-selection' || route === 'synchronice-credentials';
  }
}
