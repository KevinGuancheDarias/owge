import { LoginSessionService } from './../login-session/login-session.service';
import { Config } from './../config/config.pojo';
import { RequestObject } from '../../pojo/request-object.pojo';
import { PlanetPojo } from './../shared-pojo/planet.pojo';
import { ResourceManagerService } from './../service/resource-manager.service';
import { AutoUpdatedResources } from './../class/auto-updated-resources';
import { Http, Response, RequestOptions, URLSearchParams } from '@angular/http';
import { ErrorObservable } from 'rxjs/observable/ErrorObservable';

import { ServiceLocator } from '../service-locator/service-locator';
import { Observable } from 'rxjs';

export abstract class BaseHttpService {

  protected http;
  protected resources: AutoUpdatedResources;
  protected _selectedPlanet: PlanetPojo;

  /**
   * Inits the main base http service
   *
   * @author Kevin Guanche Darias
   */
  public constructor() {
    this.http = ServiceLocator.injector.get(Http);
  }

  protected doGet(url: string, urlSearchParams: URLSearchParams = undefined, requestOptions: RequestOptions = undefined): Observable<any> {
    const request: RequestObject = new RequestObject(urlSearchParams, requestOptions);

    return this.http.get(url, request.requestOptions)
      .map((res: Response) => {
        try {
          return res.json();
        } catch (e) {
          return null;
        }
      })
      .catch((error: Response) => this.handleError(error));
  }

  /**
   * @todo Implement this feature
   */
  protected doGetWithReplay() {
    throw new Error('Feature is not implemented');
  }

  /**
   * Will handle errors coming from http client
   *
   * @private
   * @todo Add default server pojo
   * @param {Response|any} error Error object coming from request
   * @author Kevin Guanche Darias
   */
  protected handleError(error: Response | any): ErrorObservable {
    let errMsg: string;
    if (error instanceof Response) {
      if (error.status === 0 && !error.ok) {
        errMsg = 'No se pudo conectar con el servidor, comprueba tu conexión a Internet';
      } else {
        errMsg = this.translateServerError(error);
      }
    } else {
      errMsg = error.message ? error.message : error.toString();
    }
    return Observable.throw(errMsg);
  }

  /**
   * Invoked when it's confirmed connection with server was success<br />
   * Will translate the server error message to a single string
   *
   * @private
   * @param {Response} error The object from the http client
   *
   * @return {string} Translated message
   * @memberOf BaseHttpService
   */
  protected translateServerError(error: Response): string {
    try {
      const body = error.json() || ''; // Should have a default server exception pojo
      return body.message ? body.message : 'El servidor no respondió correctamente';
    } catch (e) {
      return 'El servidor no devolvió un JSON válido';
    }
  }

  /**
   * Will auto update currentPrimaryResource and currentSecondaryResource properties
   *
   * @author Kevin Guanche Darias
   */
  protected resourcesAutoUpdate() {
    const resourceManager: ResourceManagerService = ServiceLocator.injector.get(ResourceManagerService);
    this.resources = new AutoUpdatedResources(resourceManager);
    this.resources.resourcesAutoUpdate();
  }

  /**
   *
   * @param loginSessionService
   * @param url
   * @param urlSearchParams
   * @memberOf BaseHttpService
   */
  protected httpDoGetWithAuthorization(loginSessionService: LoginSessionService, url: string,
    urlSearchParams: URLSearchParams = undefined): Observable<any> {
    const requestOptions: RequestOptions = new RequestOptions();
    requestOptions.headers = Config.genCommonFormUrlencoded();
    requestOptions.headers = loginSessionService.genHttpHeaders(requestOptions.headers);

    return this.doGet(url, urlSearchParams, requestOptions);
  }
}
