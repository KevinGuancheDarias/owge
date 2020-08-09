
import { throwError as observableThrowError, Observable, EMPTY as empty } from 'rxjs';
import { switchMap, first, catchError } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';

import { HttpOptions } from '../types/http-options.type';
import { LoggerHelper } from '../helpers/logger.helper';
import { SessionStore } from '../store/session.store';
import { ProgrammingError } from '../errors/programming.error';
import { TranslateService } from '@ngx-translate/core';
import { HttpUtil } from '../utils/http.util';

export type validNonDataMethod = 'get' | 'delete';
export type validWriteMethod = 'post' | 'put';
export type validContext = 'game' | 'admin' | 'open';

type internalValidContext = validContext | 'none';

interface ContextUrlAndStoreKey {
  url: string;
  storeKey: string;
}

/**
 * Provides common HTTP commands
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
@Injectable()
export class CoreHttpService {

  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);

  constructor(private _httpClient: HttpClient, private _sessionStore: SessionStore, private _translateService: TranslateService) {

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
    return this._httpClient.get<any>(url, options).pipe(catchError((err, caught) => this._handleObservableError(options, err, caught)));
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
    return this._doPostOrPut<T>('post', url, body, options)
      .pipe(catchError((err, caught) => this._handleObservableError(options, err, caught)));
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
    return this._doPostOrPut<T>('put', url, body, options)
      .pipe(catchError((err, caught) => this._handleObservableError(options, err, caught)));
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
    return this._httpClient.delete<any>(url, options)
      .pipe(catchError((err, caught) => this._handleObservableError(options, err, caught)));
  }

  /**
   * Sends a GET request with token authentication to the url
   *
   * @deprecated 0.8.0 As of 0.8.0 it's required to use getWithAuthorizationToContext
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T type to return
   * @param url
   * @param [options]
   * @returns
   */
  public getWithAuthorization<T = any>(url: string, options?: HttpOptions): Observable<T> {
    this._log.warnDeprecated('getWithAuthorization', '0.8.0', 'requestWithAutorizationToContext');
    return this._doGetOrDeleteWithAuthorizationToContext('none', 'get', '', url, options);
  }

  /**
   * Sends a POST request with token authentication to the url
   *
   * @deprecated 0.8.0 As of 0.8.0 it's required to use postWithAuthorizationToContext
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T
   * @param url
   * @param body
   * @param [options]
   * @returns
   */
  public postWithAuthorization<T = any>(url: string, body: any, options?: HttpOptions): Observable<T> {
    this._log.warnDeprecated('postWithAuthorization', '0.8.0', 'requestWithAutorizationToContext');
    return this._doPostOrPutWithAuthorizationToContext('none', 'post', '', url, body, options);
  }

  /**
   * Sends a PUT request with token authentication to the url
   *
   * @deprecated 0.8.0 As of 0.8.0 it's required to use putWithAuthorizationToContext
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T
   * @param url
   * @param body
   * @param [options]
   * @returns
   */
  public putWithAuthorization<T = any>(url: string, body: any, options?: HttpOptions): Observable<T> {
    this._log.warnDeprecated('putWithAuthorization', '0.8.0', 'requestWithAutorizationToContext');
    return this._doPostOrPutWithAuthorizationToContext('none', 'put', '', url, body, options);
  }

  /**
   * Sends a DELETE request with token authentication to the url
   *
   * @deprecated 0.8.0 As of 0.8.0 it's required to use deleteWithAuthorizationToContext
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @template T type to return
   * @param url
   * @param [options]
   * @returns
   */
  public deleteWithAuthorization<T = any>(url: string, options?: HttpOptions): Observable<T> {
    this._log.warnDeprecated('deleteWithAuthorization', '0.8.0', 'requestWithAutorizationToContext');
    return this._doGetOrDeleteWithAuthorizationToContext('none', 'delete', '', url, options);
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @template T
   * @param context
   *  The backend context to use, will be appended to the baseUrl,
   *  example <code>context = 'admin', baseUrl = 'http://foo/backend' , would merge into 'http://foo/backend/admin'</code> <br>
   *  <b>Notice: </b> The admin context won't work outside of admin project, because AdminUserStore MUST sync SessionStore
   * @param method HTTP method to use
   * @param baseUrl Base URL is tipically the universe URL, example http://localhost/backend_api/ (trailing lash can be ommited)
   * @param url The target URL to use, will be appended just after the <i>context</i>
   * @param [options] additional options to add, such as HTTP headers
   * @param [body] Required only when using post or put methods
   * @returns
   * @memberof CoreHttpService
   */
  public requestWithAutorizationToContext<T = any>(
    context: validContext,
    method: validNonDataMethod | validWriteMethod,
    baseUrl: string,
    url: string,
    options?: HttpOptions,
    body?: any,
  ): Observable<T> {
    if (method === 'get' || method === 'delete') {
      return this._doGetOrDeleteWithAuthorizationToContext(context, method, baseUrl, url, options);
    } else if (method === 'post' || method === 'put') {
      if (!body) {
        throw new ProgrammingError(`You can't use ${method} without specifying a body`);
      }
      return this._doPostOrPutWithAuthorizationToContext(context, method, baseUrl, url, body, options);
    } else {
      throw new ProgrammingError(`Unsupported HTTP method ${method} specified`);
    }
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
  private _translateServerError(error: Response | HttpErrorResponse | any): string {
    return HttpUtil.translateServerError(error);
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
    return this._httpClient[method](url, body, options)
      .pipe(catchError((err, caught) => this._handleObservableError(options, err, caught)));
  }

  private _doGetOrDeleteWithAuthorizationToContext<T = any>(
    context: internalValidContext,
    method: validNonDataMethod,
    base: string,
    url: string,
    options: HttpOptions
  ): Observable<T> {
    const parsedOptions: HttpOptions = this._createParsedOptions(options);
    const contextConfig: ContextUrlAndStoreKey = this._handleContextDefinition(context, base, url, false);
    return this._sessionStore.get(contextConfig.storeKey).pipe(
      first(),
      switchMap(token => {
        parsedOptions.headers = parsedOptions.headers.append('Authorization', `Bearer ${token}`);
        return this[method](contextConfig.url, parsedOptions);
      }),
      catchError((err, caught) => {
        return this._handleObservableError(options, err, caught);
      })
    );
  }

  private _doPostOrPutWithAuthorizationToContext<T = any>(
    context: internalValidContext,
    method: validWriteMethod,
    base: string,
    url: string,
    body: any,
    options?: HttpOptions
  ): Observable<T> {
    const parsedOptions: HttpOptions = this._createParsedOptions(options);
    const contextConfig: ContextUrlAndStoreKey = this._handleContextDefinition(context, base, url, false);
    return this._sessionStore.get(contextConfig.storeKey).pipe(
      first(),
      switchMap(token => {
        parsedOptions.headers = parsedOptions.headers.append('Authorization', `Bearer ${token}`);
        return this[method]<T>(contextConfig.url, body, parsedOptions);
      }),
      catchError((err, caught) => {
        return this._handleObservableError(options, err, caught);
      })
    );
  }

  private _handleContextDefinition(
    context: internalValidContext,
    baseUrl: string,
    url: string,
    allowOpen: boolean
  ): ContextUrlAndStoreKey {
    if (context === 'open' && !allowOpen) {
      throw new ProgrammingError(`Context ${context} can't be used in authenticated request`);
    } else if (context === 'admin' || context === 'game' || context === 'open') {
      return {
        url: `${baseUrl}/${context}/${url}`,
        storeKey: context === 'game' ? 'currentToken' : 'adminToken'
      };
    } else if (context === 'none') {
      return {
        url,
        storeKey: 'currentToken'
      };
    } else {
      throw new ProgrammingError('This condition should never ever happend');
    }
  }

  private _handleObservableError(options: HttpOptions, err: any, caught: Observable<any>): Observable<any> {
    if (options && options.errorHandler) {
      const result: Observable<any> = options.errorHandler(err, caught);
      if (result instanceof Observable) {
        return result;
      }
    } else {
      const errString = this._translateServerError(err);
      this._translateService.get(errString).subscribe(val => {
        console.error(`Error!\n ${val}`, err);
      });
      return empty;
    }
  }
}
