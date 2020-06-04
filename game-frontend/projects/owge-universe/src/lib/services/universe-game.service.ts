import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { first, switchMap, map, take } from 'rxjs/operators';

import {
  CoreHttpService,
  HttpOptions,
  User,
  validContext,
  validNonDataMethod,
  validWriteMethod,
  UserStorage,
  Improvement,
  StorageOfflineHelper
} from '@owge/core';
import { UniverseStorage } from '../storages/universe.storage';
import { Universe } from '../types/universe.type';
import { AbstractWebsocketApplicationHandler } from '@owge/core';

/**
 * Has common service methods directly related with the game <br>
 * This replaces the good' old <i>GameBaseService</i>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable()
export class UniverseGameService extends AbstractWebsocketApplicationHandler {
  private _offlineUserStore: StorageOfflineHelper<User> = new StorageOfflineHelper('universe_game.user');

  constructor(
    private _coreHttpService: CoreHttpService,
    private _universeStorage: UniverseStorage,
    private _userStore: UserStorage<User>
  ) {
    super();
    this._eventsMap = {
      user_data_change: '_onUserDataChange',
      user_improvements_change: '_onUserImprovementsChange'
    };
  }

  public async workaroundSync(): Promise<void> {
    const token = await this._userStore.currentToken.pipe(take(1)).toPromise();
    if (token) {
      this._onUserDataChange(
        await this.requestWithAutorizationToContext('game', 'get', 'user/findData').toPromise()
      );
    }
  }


  /**
   * <b>MUST be the first handler in the websocket</b>
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returns
   */
  public async workaroundInitialOffline(): Promise<void> {
    this._offlineUserStore.doIfNotNull(user => this._userStore.currentUser.next(this._handleUserLoad(user)));
  }

  /**
   * Finds the logged in current user data
   * <b>NOTICE:</b>, Will set PlanetStore.selectedPlanet to user home planet
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @template T
   * @returns
   */
  public findLoggedInUserData<T extends User>(): Observable<T> {
    return <any>this._userStore.currentUser.asObservable();
  }

  /**
   * Executes a GET query to the universe <b>WITHOUT</b> authentication
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.3
   * @template T
   * @param  url
   * @param  [options]
   * @returns
   */
  public getToUniverse<T = any>(url: string, options?: HttpOptions): Observable<T> {
    return this._universeStorage.currentUniverse.pipe(
      first(),
      switchMap(
        currentUniverse => this._coreHttpService.get(`${currentUniverse.restBaseUrl}/${url}`, options)
      )
    );
  }

  /**
   * Sends a GET request to current universe
   *
   * @deprecated As of 0.8.0 Use requestWithAutorizationToContext() instead
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T
   * @param url
   * @param [options]
   * @returns
   */
  public getWithAuthorizationToUniverse<T = any>(url: string, options?: HttpOptions): Observable<T> {
    this._log.warnDeprecated('getWithAuthorizationToUniverse()', '0.8.0', 'requestWithAutorizationToContext()');
    return this._getDeleteWithAuthorizationToContext<T>('game', 'get', url, options);
  }

  /**
   * Sends a POST request to current universe
   *
   * @deprecated As of 0.8.0 Use requestWithAutorizationToContext() instead
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T
   * @param url
   * @param [body]
   * @param [options]
   * @returns
   */
  public postWithAuthorizationToUniverse<T = any>(url: string, body?: any, options?: HttpOptions): Observable<T> {
    this._log.warnDeprecated('postWithAuthorizationToUniverse()', '0.8.0', 'requestWithAutorizationToContext()');
    return this._postPutWithAuthorizationToContext<T>('game', 'post', url, body, options);
  }

  /**
   * Sends a PUT request to current universe
   *
   * @deprecated As of 0.8.0 Use requestWithAutorizationToContext() instead
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T
   * @param url
   * @param body
   * @param [options]
   */
  public putwithAuthorizationToUniverse<T = any>(url: string, body: any, options?: HttpOptions): Observable<T> {
    this._log.warnDeprecated('putWithAuthorizationToUniverse()', '0.8.0', 'requestWithAutorizationToContext()');
    return this._postPutWithAuthorizationToContext<T>('game', 'put', url, body, options);
  }

  /**
   * Sends a DELETE request to current universe
   *
   * @deprecated As of 0.8.0 Use requestWithAutorizationToContext() instead
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T
   * @param url
   * @param [options]
   * @returns
   */
  public deleteWithAuthorizationToUniverse<T = any>(url: string, options?: HttpOptions): Observable<T> {
    this._log.warnDeprecated('deleteWithAuthorizationToUniverse()', '0.8.0', 'requestWithAutorizationToContext()');
    return this._getDeleteWithAuthorizationToContext<T>('game', 'delete', url, options);
  }

  /**
   * Executes an HTTP request to the currently selected universe
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @template T
   * @param context
   *  The backend context to use, will be appended to the baseUrl,
   *  example <code>context = 'admin', baseUrl = 'http://foo/backend' , would merge into 'http://foo/backend/admin'</code>
   * @param method HTTP method to use
   * @param url The target URL to use, will be appended just after the <i>context</i>
   * @param [body] Required only when using post or put methods
   * @param [options] additional options to add, such as HTTP headers
   * @returns
   * @memberof UniverseGameService
   */
  public requestWithAutorizationToContext<T = any>(
    context: validContext,
    method: validNonDataMethod | validWriteMethod,
    url: string,
    body?: any,
    options?: HttpOptions
  ): Observable<T> {
    return this._universeStorage.currentUniverse.pipe<Universe, any>(
      first(),
      switchMap(
        currentUniverse =>
          this._coreHttpService.requestWithAutorizationToContext<T>(context, method, currentUniverse.restBaseUrl, url, options, body)
      )
    );
  }

  protected _onUserDataChange(user: User) {
    this._userStore.currentUser.next(this._handleUserLoad(user));
    this._offlineUserStore.save(user);
  }

  protected _onUserImprovementsChange(content: Improvement): void {
    this._userStore.currentUserImprovements.next(content);
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @template T
   * @param contextPrefix If the context is game, admin, or open
   * @param method
   * @param url
   * @param options
   * @returns
   */
  private _getDeleteWithAuthorizationToContext<T = any>(
    contextPrefix: validContext,
    method: validNonDataMethod,
    url: string,
    options?: HttpOptions
  ): Observable<T> {
    return this._universeStorage.currentUniverse.pipe<Universe, any>(
      first(),
      switchMap(
        currentUniverse =>
          this._coreHttpService[`${method}WithAuthorization`](`${currentUniverse.restBaseUrl}/${contextPrefix}/${url}`, options)
      )
    );
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @template T
   * @param contextPrefix
   * @param method
   * @param url
   * @param body
   * @param [options]
   * @returns
   */
  private _postPutWithAuthorizationToContext<T = any>(
    contextPrefix: validContext,
    method: validWriteMethod,
    url: string,
    body: any,
    options?: HttpOptions
  ): Observable<T> {
    return this._universeStorage.currentUniverse.pipe<Universe, any>(
      first(),
      switchMap(
        currentUniverse =>
          this._coreHttpService[`${method}WithAuthorization`](`${currentUniverse.restBaseUrl}/${contextPrefix}/${url}`, body, options)
      )
    );
  }


  /**
   * Because backend still sends as user property, factionDto instead of faction, we have to fix it here
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param  backendUser
   */
  private _workaroundFactionFix(backendUser: any): void {
    if (backendUser.factionDto && !backendUser.faction) {
      backendUser.faction = backendUser.factionDto;
    }
  }

  private _handleUserLoad(user: User): User {
    if (!user.consumedEnergy) {
      user.consumedEnergy = 0;
    }
    this._workaroundFactionFix(user);
    this._onUserImprovementsChange(user.improvements);
    return user;
  }
}
