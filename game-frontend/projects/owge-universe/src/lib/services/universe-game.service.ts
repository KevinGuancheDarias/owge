import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { first ,  switchMap, map } from 'rxjs/operators';

import { CoreHttpService, HttpOptions, User, SessionStore } from '@owge/core';
import { UniverseStorage } from '../storages/universe.storage';
import { Universe } from '../types/universe.type';

type validNonDataMethod = 'get' | 'delete';
type validWriteMethod = 'post' | 'put';

/**
 * Has common service methods directly related with the game <br>
 * This replaces the good' old <i>GameBaseService</i>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable()
export class UniverseGameService {

  constructor(
    private _coreHttpService: CoreHttpService,
    private _universeStorage: UniverseStorage,
    private _sessionStore: SessionStore
  ) { }


  /**
   * Finds the logged in current user
   * <b>NOTICE:</b>, Will set PlanetStore.selectedPlanet to user home planet
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @template T
   * @returns
   */
  public findLoggedInUserData<T extends User>(): Observable<T> {
    return this.getWithAuthorizationToUniverse('user/findData').pipe(
    map(current => {
      if (!current.consumedEnergy) {
        current.consumedEnergy = 0;
      }
      this._workaroundFactionFix(current);
      this._sessionStore.next('selectedPlanet', current.homePlanetDto);
      return current;
    }));
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
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T
   * @param url
   * @param [options]
   * @returns
   */
  public getWithAuthorizationToUniverse<T = any>(url: string, options?: HttpOptions): Observable<T> {
    return this._getDeleteWithAuthorizationToUuniverse<T>('get', url, options);
  }

  /**
   * Sends a POST request to current universe
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T
   * @param url
   * @param [body]
   * @param [options]
   * @returns
   */
  public postwithAuthorizationToUniverse<T = any>(url: string, body?: any, options?: HttpOptions): Observable<T> {
    return this._postPutWithAuthorizationToUniverse<T>('post', url, body, options);
  }

  /**
   * Sends a PUT request to current universe
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T
   * @param url
   * @param body
   * @param [options]
   */
  public putwithAuthorizationToUniverse<T = any>(url: string, body: any, options?: HttpOptions): Observable<T> {
    return this._postPutWithAuthorizationToUniverse<T>('put', url, body, options);
  }

  /**
   * Sends a DELETE request to current universe
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T
   * @param url
   * @param [options]
   * @returns
   */
  public deleteWithAuthorizationToUniverse<T = any>(url: string, options?: HttpOptions): Observable<T> {
    return this._getDeleteWithAuthorizationToUuniverse<T>('delete', url, options);
  }

  private _getDeleteWithAuthorizationToUuniverse<T = any>(method: validNonDataMethod, url: string, options: HttpOptions): Observable<T> {
    return this._universeStorage.currentUniverse.pipe<Universe, any>(
      first(),
      switchMap(
        currentUniverse => this._coreHttpService[`${method}WithAuthorization`](`${currentUniverse.restBaseUrl}/${url}`, options)
      )
    );
  }

  private _postPutWithAuthorizationToUniverse<T = any>(
    method: validWriteMethod,
    url: string,
    body: any,
    options?: HttpOptions
  ): Observable<T> {
    return this._universeStorage.currentUniverse.pipe<Universe, any>(
      first(),
      switchMap(
        currentUniverse => this._coreHttpService[`${method}WithAuthorization`](`${currentUniverse.restBaseUrl}/${url}`, body, options)
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
}
