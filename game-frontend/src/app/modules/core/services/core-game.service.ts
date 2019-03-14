import { Injectable } from '@angular/core';

import { Observable } from 'rxjs/Observable';
import { first } from 'rxjs/operators/first';

import { CoreHttpService } from './core-http.service';
import { HttpOptions } from '../types/http-options.type';
import { UniverseStorage } from '../../universe/storages/universe.storage';
import { switchMap } from 'rxjs/operators/switchMap';

type validNonDataMethod = 'get' | 'delete';
type validWriteMethod = 'post' | 'put';

/**
 * Has common service methods directly related with the game <br>
 * This replaces the good' old <i>GameBaseService</i>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @class CoreGameService
 */
@Injectable()
export class CoreGameService {

  constructor(private _coreHttpService: CoreHttpService, private _universeStorage: UniverseStorage) {

  }


  /**
   * Executes a GET query to the universe <b>WITHOUT</b> authentication
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.3
   * @template T
   * @param {string} url
   * @param {HttpOptions} [options]
   * @returns {Observable<T>}
   * @memberof CoreGameService
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
   * @param {string} url
   * @param {HttpOptions} [options]
   * @returns {Observable<T>}
   * @memberof CoreGameService
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
   * @param {string} url
   * @param {*} [body]
   * @param {HttpOptions} [options]
   * @returns {Observable<T>}
   * @memberof CoreGameService
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
   * @param {string} url
   * @param {*} body
   * @param {HttpOptions} [options]
   * @returns {Observable<T>}
   * @memberof CoreGameService
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
   * @param {string} url
   * @param {HttpOptions} [options]
   * @returns {Observable<T>}
   * @memberof CoreGameService
   */
  public deleteWithAuthorizationToUniverse<T = any>(url: string, options?: HttpOptions): Observable<T> {
    return this._getDeleteWithAuthorizationToUuniverse<T>('delete', url, options);
  }

  private _getDeleteWithAuthorizationToUuniverse<T = any>(method: validNonDataMethod, url: string, options: HttpOptions): Observable<T> {
    return this._universeStorage.currentUniverse.pipe(
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
    return this._universeStorage.currentUniverse.pipe(
      first(),
      switchMap(
        currentUniverse => this._coreHttpService[`${method}WithAuthorization`](`${currentUniverse.restBaseUrl}/${url}`, body, options)
      )
    );
  }
}
