
import {throwError as observableThrowError,  Observable ,  EMPTY as empty } from 'rxjs';
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { switchMap ,  first ,  catchError } from 'rxjs/operators';

import { HttpOptions } from '../types/http-options.type';
import { UserStorage } from '../storages/user.storage';
import { User } from '../types/user.type';

type validNonDataMethod = 'get' | 'delete';
type validWriteMethod = 'post' | 'put';

/**
 * Provides common HTTP commands
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
@Injectable()
export class CoreHttpService {

  constructor(private _httpClient: HttpClient, private _userStorage: UserStorage<User>) {

  }

  /**
   * Gets an HTTP resource <br>
   * Notice: Will translate server error
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T
   * @param url
   * @param [options] Options to use in the request
   * @returns
   */
  public get<T = any>(url: string, options?: HttpOptions): Observable<T> {
    return this._httpClient.get<any>(url, options).pipe(catchError(error => this._handleError(error)));
  }


  /**
   * POSTs an HTTP resource <br>
   * Notice: Will translate server error
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T
   * @param url
   * @param body
   * @param [options]
   * @returns
   */
  public post<T = any>(url: string, body: any, options?: HttpOptions): Observable<T> {
    return this._doPostOrPut<T>('post', url, body, options);
  }

  /**
   * PUTs an HTTP resource <br>
   * Notice: Will translate server error
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T
   * @param url
   * @param body
   * @param [options]
   * @returns
   */
  public put<T = any>(url: string, body: any, options?: HttpOptions): Observable<T> {
    return this._doPostOrPut<T>('put', url, body, options);
  }

  /**
   * DELETEs an HTTP resource <br>
   * Notice: Will translate server error
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T
   * @param url
   * @param [options] Options to use in the request
   * @returns
   */
  public delete<T = any>(url: string, options?: HttpOptions): Observable<T> {
    return this._httpClient.delete<any>(url, options).pipe(catchError(error => this._handleError(error)));
  }

  /**
   * Sends a GET request with token authentication to the url
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T type to return
   * @param url
   * @param [options]
   * @returns
   */
  public getWithAuthorization<T = any>(url: string, options?: HttpOptions): Observable<T> {
    return this._doGetOrDeleteWithAuthorization('get', url, options);
  }

  /**
   * Sends a POST request with token authentication to the url
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T
   * @param url
   * @param body
   * @param [options]
   * @returns
   */
  public postWithAuthorization<T = any>(url: string, body: any, options?: HttpOptions): Observable<T> {
    return this._doPostOrPutWithAuthorization('post', url, body, options);
  }

  /**
   * Sends a PUT request with token authentication to the url
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T
   * @param url
   * @param body
   * @param [options]
   * @returns
   */
  public putWithAuthorization<T = any>(url: string, body: any, options?: HttpOptions): Observable<T> {
    return this._doPostOrPutWithAuthorization('put', url, body, options);
  }

  /**
   * Sends a DELETE request with token authentication to the url
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T type to return
   * @param url
   * @param [options]
   * @returns
   */
  public deleteWithAuthorization<T = any>(url: string, options?: HttpOptions): Observable<T> {
    return this._doGetOrDeleteWithAuthorization('delete', url, options);
  }

  /**
   * Will handle errors coming from http client
   *
   * @todo Add default server pojo
   * @param error Error object coming from request
   * @author Kevin Guanche Darias
   */
  private _handleError(error: Response | any): Observable<any> {
    let errMsg: string;
    if (error instanceof Response) {
      if (error.status === 0 && !error.ok) {
        errMsg = 'No se pudo conectar con el servidor, comprueba tu conexión a Internet';
      } else {
        errMsg = this._translateServerError(error);
      }
    } else if (error.error && error.error.exceptionType) {
      errMsg = this._translateServerError(error.error);
    } else {
      errMsg = error.message ? error.message : error.toString();
    }
    return observableThrowError(errMsg);
  }

  /**
   * Invoked when it's confirmed connection with server was success<br />
   * Will translate the server error message to a single string
   *
   * @param error The object from the http client
   *
   * @return Translated message
   * @memberOf BaseHttpService
   */
  private _translateServerError(error: Response | any): string {
    try {
      let body: any = {};
      if (error instanceof Response) {
        body = error.json() || ''; // Should have a default server exception pojo
      } else if (error.exceptionType && error.message) {
        body = error;
      }
      return body.message ? body.message : 'El servidor no respondió correctamente';
    } catch (e) {
      return 'El servidor no devolvió un JSON válido';
    }
  }

  /**
   * Ensures a correct options object is returned even in null <i>options</i> input
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param [options]
   * @returns A clone of the input options, or new if null
   */
  private _createParsedOptions(options?: HttpOptions): HttpOptions {
    const parsedOptions: HttpOptions = { ...options } || {};
    parsedOptions.headers = parsedOptions.headers
      ? parsedOptions.headers
      : new HttpHeaders();
    return parsedOptions;
  }

  private _doPostOrPut<T = any>(method: validWriteMethod, url: string, body: any, options: HttpOptions): Observable<T> {
    return this._httpClient[method](url, body, options).pipe(catchError(error => this._handleError(error)));
  }

  private _doGetOrDeleteWithAuthorization<T = any>(method: validNonDataMethod, url: string, options: HttpOptions): Observable<T> {
    const parsedOptions: HttpOptions = this._createParsedOptions(options);
    return this._userStorage.currentToken.pipe(
      first(),
      switchMap(token => {
        parsedOptions.headers = parsedOptions.headers.append('Authorization', `Bearer ${token}`);
        return this[method](url, parsedOptions);
      }),
      catchError((err, caught) => {
        return this._handleObservableError(options, err, caught);
      })
    );
  }

  private _doPostOrPutWithAuthorization<T = any>(method: validWriteMethod, url: string, body: any, options?: HttpOptions): Observable<T> {
    const parsedOptions: HttpOptions = this._createParsedOptions(options);
    return this._userStorage.currentToken.pipe(
      first(),
      switchMap(token => {
        parsedOptions.headers = parsedOptions.headers.append('Authorization', `Bearer ${token}`);
        return this[method]<T>(url, body, parsedOptions);
      }),
      catchError((err, caught) => {
        return this._handleObservableError(options, err, caught);
      })
    );
  }

  private _handleObservableError(options: HttpOptions, err: any, caught: Observable<any>): Observable<any> {
    if (options && options.errorHandler) {
      const result: Observable<any> = options.errorHandler(err, caught);
      if (result instanceof Observable) {
        return result;
      }
    } else {
      alert(`Error!\n ${err}`);
      return empty;
    }
  }
}
