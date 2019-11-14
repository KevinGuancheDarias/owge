import { Injectable, Injector } from '@angular/core';
import { Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { skip, map } from 'rxjs/operators';

import { UserStorage, LoggerHelper, CoreHttpService, ProgrammingError, User } from '@owge/core';
import { UniverseStorage, Universe, UniverseGameService } from '@owge/universe';

import { PlanetPojo } from './../shared-pojo/planet.pojo';
import { TokenPojo } from './token.pojo';
import { UserPojo } from '../shared-pojo/user.pojo';
import { Observable, BehaviorSubject } from 'rxjs';
import { ResourceManagerService } from './../service/resource-manager.service';
import { Faction } from '../shared-pojo/faction.pojo';
import { HttpHeaders } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { MissionService } from '../services/mission.service';

/**
 * Provides token storage and retrieving features<br />
 * Also provide session concept storage
 *
 * @deprecated This class is intented to be removed, most of its functionality will be marked as deprecated
 * @export
 * @class LoginSessionService
 */
@Injectable()
export class LoginSessionService implements CanActivate {
  public static readonly LOCAL_STORAGE_TOKEN_PARAM = 'owge_authentication';
  public static readonly LOCAL_STORAGE_SELECTED_UNIVERSE = 'owge_universe';
  public static readonly LOCAL_STORAGE_SELECTED_FACTION = 'owge_faction';
  public static readonly LOGIN_ROUTE = '/login';

  private alreadyNotified = false;


  /**
   * Represents the user data, for logged in user, loaded after method invocation!
   *
   * @deprecated As of 0.7 it's better to use UserModule/UserStorage.currentUser instead
   * @readonly
   * @type {BehaviorSubject<UserPojo>}
   * @memberof LoginSessionService
   */
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

  private _lgsLog: LoggerHelper = new LoggerHelper(this.constructor.name);

  constructor(private _injector: Injector,
    private _resourceManagerService: ResourceManagerService,
    private _userStorage: UserStorage<User>,
    private _universeStorage: UniverseStorage,
    private _universeGameService: UniverseGameService,
    private _missionService: MissionService
  ) {
    this._workaroundUserStorage();
    this._workaroundUniverseStorage();
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
    this._userStorage.currentToken.next(token);
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
    const loadingRoute = route.url[0].path;
    if (this.hasLoginDomain()) {
      if ((this.isLoginDomain() && this.isLoggedIn() && !this._isLoginRoute(loadingRoute))
        || (!this.isLoginDomain() && !this.isLoggedIn())) {
        this.logout();
        window.location.href = `//${environment.loginDomain}`;
        return false;
      } else if ((!this.isLoginDomain() && this.isLoggedIn())
        || (this.isLoginDomain() && this.isLoggedIn() && this._isLoginRoute(loadingRoute))) {
        this._handleLoginLogic(loadingRoute);
        return true;
      } else if (this.isLoginDomain() && !this._isLoginRoute(loadingRoute)) {
        this.logout();
      } else {
        throw new ProgrammingError('Situation not expected');
      }
    } else {
      this._redirectIfNotLoggedIn();
      this._handleLoginLogic(loadingRoute);
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
   * @deprecated This feature is not in use in modern <i>CoreHttpService</i>
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {HttpHeaders} [headers] If present will append to existing, else will create new
   * @returns {HttpHeaders}
   * @memberof LoginSessionService
   */
  public genHttpClientHeaders(headers?: HttpHeaders): HttpHeaders {
    this._lgsLog.warnDeprecated('genHttpClientHeaders()', '0.7.0', 'Don\'t use LoginSessionService');
    const target: HttpHeaders = headers
      ? headers
      : new HttpHeaders();
    return target.append('Authorization', `Bearer ${this.getRawToken()}`);
  }


  /**
   *
   * @deprecated As of 0.8.0 it's better to use the same method but from UniverseGameService
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.3.0
   * @returns
   */
  public findLoggedInUserData(): Observable<UserPojo> {
    this._lgsLog.warnDeprecated('findLoggedInUserData', '0.8.0', 'UniverseGameService.findLoggedInUserData()');
    return this._universeGameService.getWithAuthorizationToUniverse('user/findData').pipe(
      map(current => {
        if (!current.consumedEnergy) {
          current.consumedEnergy = 0;
        }
        return current;
      }));
  }

  public getSelectedUniverse(): Universe {
    return JSON.parse(sessionStorage.getItem(LoginSessionService.LOCAL_STORAGE_SELECTED_UNIVERSE));
  }


  /**
   *
   * @deprecated As of 0.8.0 it's better to use OwgeUniverse/UniverseService.setSelectedUniverse
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param universe
   * @memberof LoginSessionService
   */
  public setSelectedUniverse(universe: Universe) {
    this._lgsLog.warnDeprecated('setSelectedUniverse()', '0.8.0', 'OwgeUniverse/UniverseService.setSelectedUniverse');
    this._universeStorage.currentUniverse.next(universe);
    sessionStorage.setItem(LoginSessionService.LOCAL_STORAGE_SELECTED_UNIVERSE, JSON.stringify(universe));
  }

  public getSelectedFaction(): Faction {
    return JSON.parse(sessionStorage.getItem(LoginSessionService.LOCAL_STORAGE_SELECTED_FACTION));
  }

  public setSelectedFaction(faction: Faction) {
    sessionStorage.setItem(LoginSessionService.LOCAL_STORAGE_SELECTED_FACTION, JSON.stringify(faction));
  }

  public getSelectedPlanet(): PlanetPojo {
    return this._findSelectedPlanet.value;
  }

  public defineSelectedPlanet(planet: PlanetPojo): void {
    this._findSelectedPlanet.next(planet);
  }

  public logout(): any {
    this._clearSessionData();
    this.alreadyNotified = false;
    this._redirectIfNotLoggedIn();
  }

  public hasLoginDomain(): boolean {
    return !!environment.loginDomain;
  }

  public isLoginDomain(): boolean {
    if (!this.hasLoginDomain) {
      throw new ProgrammingError('Can NOT invoke this method when the env has no login route');
    }
    return environment.loginDomain === window.document.domain;
  }

  /**
   * Will return the parsed Token
   *
   * @param {string} jwtToken - The jwt token
   * @return {TokenPojo} the encoded token
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

  private _redirectTo(route: string): void {
    const router = this._injector.get(Router);
    router.navigate([route]);
  }
  private _redirectIfNotLoggedIn(): void {
    if (!this.isLoggedIn()) {
      console.log('LoginSessionService: Redirecting to ' + LoginSessionService.LOGIN_ROUTE);
      this._isInGame.next(false);
      this._redirectTo(LoginSessionService.LOGIN_ROUTE);
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
    this._universeGameService.findLoggedInUserData<any>().subscribe(
      userData => {
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

  private _isLoginRoute(route: string): boolean {
    return route === 'universe-selection' || route === 'faction-selection' || route === 'synchronice-credentials';
  }

  private _handleLoginLogic(loadingRoute: string): void {
    if (!this._isLoginRoute(loadingRoute) && !this.alreadyNotified) {
      this._notifyGameFrontendCore();
      this.alreadyNotified = true;
    } else if (!this._isLoginRoute(loadingRoute)) {
      this._workaroundMissionsCount();
    }
  }

  private _workaroundMissionsCount(): void {
    this._lgsLog.todo(['Replace workaroundMissionsCount for a websocket "Adapter" that listens for mission count changes']);
    this._missionService.loadCount();
  }

  /**
   * Due to deprecation of this.userData, we have to make the new way be working by default <br>
   * In a new major version userData will be removed
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @private
   * @memberof LoginSessionService
   */
  private _workaroundUserStorage(): void {
    this._userData.pipe(skip(1)).subscribe(value => this._userStorage.currentUser.next(<any>value));
    const currentToken: string = this.getRawToken();
    if (currentToken) {
      this._userStorage.currentToken.next(currentToken);
    }
  }

  /**
   * Due to future deprecation of LoginSessionService, I workaround the universeStorage
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @private
   * @returns {*}
   * @memberof LoginSessionService
   */
  private _workaroundUniverseStorage(): any {
    this._universeStorage.currentUniverse.next(this.getSelectedUniverse());
  }
}
