import { PlanetPojo } from './../shared-pojo/planet.pojo';
import { Injectable, Injector } from '@angular/core';
import { Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Headers } from '@angular/http';

import { TokenPojo } from './token.pojo';
import { UserPojo } from '../shared-pojo/user.pojo';
import { Universe } from '../shared-pojo/universe.pojo';
import { Observable } from 'rxjs/Observable';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { ResourceManagerService } from './../service/resource-manager.service';
import { Faction } from '../shared-pojo/faction.pojo';
import { BaseHttpService } from '../base-http/base-http.service';
import { HttpHeaders } from '@angular/common/http';
import { WebsocketService } from '../service/websocket.service';

/**
 * Provides token storage and retrieving features<br />
 * Also provide session concept storage
 *
 * @export
 * @class LoginSessionService
 */
@Injectable()
export class LoginSessionService extends BaseHttpService implements CanActivate {
  public static readonly LOCAL_STORAGE_TOKEN_PARAM = 'sgt_authentication';
  public static readonly LOCAL_STORAGE_SELECTED_UNIVERSE = 'sgt_universe';
  public static readonly LOCAL_STORAGE_SELECTED_FACTION = 'sgt_faction';
  public static readonly LOGIN_ROUTE = '/login';

  private alreadyNotified = false;

  /** @var {UserPojo} Represents the user data, for logged in user, loaded after method invocation! */
  public get userData(): BehaviorSubject<UserPojo> {
    return this._userData;
  }
  private _userData: BehaviorSubject<UserPojo> = new BehaviorSubject(null);

  public get isInGame(): Observable<boolean> {
    return this._isInGame.asObservable();
  }
  private _isInGame: BehaviorSubject<boolean> = new BehaviorSubject(false);

  public get findSelectedPlanet(): Observable<PlanetPojo> {
    return this._findSelectedPlanet.asObservable();
  }
  private _findSelectedPlanet: BehaviorSubject<PlanetPojo> = new BehaviorSubject(null);

  constructor(private _injector: Injector,
    private _resourceManagerService: ResourceManagerService,
    private _websocketService: WebsocketService
  ) {
    super();
  }

  public findTokenData(): UserPojo {
    let retVal: UserPojo;
    const token: TokenPojo = this._findTokenIfNotExpired();
    if (token) {
      retVal = token.data;
    }
    return retVal;
  }

  /**
   * @param {string} token - RAW encoded token
   * @return {TokenPojo} the encoded token
   */
  public setTokenPojo(token): void {
    sessionStorage.setItem(LoginSessionService.LOCAL_STORAGE_TOKEN_PARAM, token);
  }

  public getParsedToken(): TokenPojo {
    const jwtToken: string = sessionStorage.getItem(LoginSessionService.LOCAL_STORAGE_TOKEN_PARAM);
    return this._parseToken(jwtToken);
  }

  public getRawToken() {
    return sessionStorage.getItem(LoginSessionService.LOCAL_STORAGE_TOKEN_PARAM);
  }

  public isLoggedIn(): boolean {
    this._removeSessionStorageDataIfSessionExpired();
    return !!this._findTokenIfNotExpired();
  }

  /**
   * Returns true when the user has logged in and, has selected universe, and has selected faction
   *
   * @author Kevin Guanche Darias
   */
  public checkIsInGame(): boolean {
    return this.isLoggedIn() && !!this.getSelectedUniverse() && !!this.getSelectedFaction();
  }

  /**
   * Will check if the route can be loaded <br>
   * IMPORTANT: <b>If not logged in will redirect to login URL </b>
   *
   * @param route Route to test if can be activated
   * @param state unused but required by the Angular router
   * @author Kevin Guanche Darias
   */
  public canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
    this._redirectIfNotLoggedIn();
    const loadingRoute = route.url[0].path;
    if (loadingRoute !== 'universe-selection' && loadingRoute !== 'faction-selection' && !this.alreadyNotified) {
      this._notifyGameFrontendCore();
      this._websocketService.authenticate(this.getRawToken());
      this.alreadyNotified = true;
    }
    return this.isLoggedIn();
  }

  /**
   * Generates the HTTP headers with the Authorization token included
   *
   * @deprecated Use genHttpClientHeaders
   * @param {Headers} [headers] If present will append to existing, else will create new
   * @return {Headers}
   */
  public genHttpHeaders(headers?: Headers): Headers {
    if (!headers) {
      headers = new Headers();
    }
    headers.append('Authorization', 'Bearer ' + this.getRawToken());
    return headers;
  }

  /**
   * Generates the HTTP headers with the Authorization token included
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {HttpHeaders} [headers] If present will append to existing, else will create new
   * @returns {HttpHeaders}
   * @memberof LoginSessionService
   */
  public genHttpClientHeaders(headers?: HttpHeaders): HttpHeaders {
    const target: HttpHeaders = headers
      ? headers
      : new HttpHeaders();
    return target.append('Authorization', `Bearer ${this.getRawToken()}`);
  }
  public findLoggedInUserData(): Observable<UserPojo> {
    return this.httpDoGetWithAuthorization(this, this.getSelectedUniverse().restBaseUrl + '/user/findData');
  }

  public getSelectedUniverse(): Universe {
    return JSON.parse(sessionStorage.getItem(LoginSessionService.LOCAL_STORAGE_SELECTED_UNIVERSE));
  }

  public setSelectedUniverse(universe: Universe) {
    sessionStorage.setItem(LoginSessionService.LOCAL_STORAGE_SELECTED_UNIVERSE, JSON.stringify(universe));
  }

  public getSelectedFaction(): Faction {
    return JSON.parse(sessionStorage.getItem(LoginSessionService.LOCAL_STORAGE_SELECTED_FACTION));
  }

  public setSelectedFaction(faction: Faction) {
    sessionStorage.setItem(LoginSessionService.LOCAL_STORAGE_SELECTED_FACTION, JSON.stringify(faction));
  }

  public defineSelectedPlanet(planet: PlanetPojo): void {
    this._findSelectedPlanet.next(planet);
  }

  public logout(): any {
    this._clearSessionData();
    this.alreadyNotified = false;
    this._redirectIfNotLoggedIn();
  }

  /**
   * Will return the parsed Token
   *
   * @param {string} jwtToken - The jwt token
   * @return {TokenPojo} the encoded token
   * @author Kevin Guanche Darias
   */
  private _parseToken(jwtToken: string): TokenPojo {
    return JSON.parse(atob(jwtToken.split('.')[1]));
  }

  /**
   * Will return token IF not expired<br />
   * Notice: removes token from sessionStorage if expired
   *
   * @private
   * @returns {TokenPojo}
   *
   * @memberOf LoginSessionService
   * @author Kevin Guanche Darias
   */
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

  private _redirectIfNotLoggedIn() {
    if (!this.isLoggedIn()) {
      console.log('LoginSessionService: Redirecting to ' + LoginSessionService.LOGIN_ROUTE);
      this._isInGame.next(false);
      const router = this._injector.get(Router);
      router.navigate([LoginSessionService.LOGIN_ROUTE]);
    }
  }
  private _removeSessionStorageDataIfSessionExpired() {
    if (!this._findTokenIfNotExpired()) {
      this._clearSessionData();
    }
  }

  /**
   * Notifies the AppComponent that we are ready to play!<br />
   * It also loads logged in user data
   *
   * @author Kevin Guanche Darias
   */
  private _notifyGameFrontendCore() {
    this._isInGame.next(true);
    this._findUserData();
  }

  /**
   * Will obtain the logged in user data <br />
   * Such as resources, planets, etc!<br />
   * IMPORTANT: It notifies the AppComponent
   *
   * @author Kevin Guanche Darias
   */
  private _findUserData(): void {
    this.findLoggedInUserData().subscribe(
      (userData) => {
        this._findSelectedPlanet.next(userData.homePlanetDto);
        this._userData.next(userData);
        this._resourceManagerService.startHandling(userData);
      },
      error => alert(error)
    );
  }

  private _clearSessionData() {
    sessionStorage.removeItem(LoginSessionService.LOCAL_STORAGE_TOKEN_PARAM);
    sessionStorage.removeItem(LoginSessionService.LOCAL_STORAGE_SELECTED_UNIVERSE);
    sessionStorage.removeItem(LoginSessionService.LOCAL_STORAGE_SELECTED_FACTION);
  }
}
