import { URLSearchParams } from '@angular/http';
import { Observable } from 'rxjs/Observable';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

import { PlanetPojo } from '../shared-pojo/planet.pojo';
import { Universe } from './../shared-pojo/universe.pojo';
import { LoginSessionService } from './../login-session/login-session.service';
import { ServiceLocator } from './../service-locator/service-locator';
import { BaseHttpService } from '../base-http/base-http.service';

/**
 * This class has the http base, and extends it adding game related contents <br>
 * This service adds:
 * <ul>
 * <li>Selected planet detector</li>
 * <li>LoginSessionService</li>
 * <li>Auto updated resources Observable</li>
 * </ul>
 * @export
 * @class GameBaseService
 * @template E target Entity object to be "behaviorsubject loadable"
 * @extends BaseHttpService
 */
export class GameBaseService<E = any> extends BaseHttpService {

  protected _loginSessionService: LoginSessionService;
  protected _loadableBehaviorSubject: BehaviorSubject<E[]> = new BehaviorSubject(null);

  constructor() {
    super();
    this._loginSessionService = ServiceLocator.injector.get(LoginSessionService);
  }

  public getLoginSessionService(): LoginSessionService {
    return this._loginSessionService;
  }

  public setLogginSessionService(loginSessionService: LoginSessionService): void {
    this._loginSessionService = loginSessionService;
  }

  public async waitForSelectedPlanet(): Promise<void> {
    await this._findSelectedPlanet;
  }

  protected doGetWithAuthorization(url: string, urlSearchParams?: URLSearchParams): Observable<any> {
    return this.httpDoGetWithAuthorization(this._loginSessionService, url, urlSearchParams);
  }

  protected _doPostWithAuthorization(url: string, body: any): Observable<any> {
    return this._httpDoPostWithAuthorization(this._loginSessionService, url, body);
  }

  /**
   * Will do exactly the same that doGetWithAuthorization() but appending universe URL to the beggining
   */
  protected doGetWithAuthorizationToGame<T = any>(url: string, urlSearchParams?: URLSearchParams): Observable<T> {
    const absoluteUrl: string = this.getUniverseUrl() + '/' + url;
    return this.doGetWithAuthorization(absoluteUrl, urlSearchParams);
  }

  protected _doPostWithAuthorizationToGame<B = any>(url: string, body: B): Observable<any> {
    const absoluteUrl: string = this.getUniverseUrl() + '/' + url;
    return this._doPostWithAuthorization(absoluteUrl, body);
  }

  /**
   * Finds the universe url, null safe
   *
   * @author Kevin Guanche Darias
   */
  protected getUniverseUrl(): string {
    const universe: Universe = this.getLoginSessionService().getSelectedUniverse();
    return universe ? universe.restBaseUrl : '';
  }

  /**
   * Will auto-update the _selectedPlanet protected property
   * <br>
   * Based on changes coming from LoginSessionService
   *
   * @author Kevin Guanche Darias
   */
  protected _requireSelectedPlanet(): void {
    this._loginSessionService.findSelectedPlanet.subscribe(selectedPlanet => this._selectedPlanet = selectedPlanet);
  }

  protected _findSelectedPlanet(): Promise<PlanetPojo> {
    return new Promise(resolve => {
      this._loginSessionService.findSelectedPlanet.filter(planet => planet !== null).subscribe(planet => resolve(planet));
    });
  }

  protected _subjectToObservable(): Observable<E[]> {
    return this._loadableBehaviorSubject.asObservable().filter(value => value !== null);
  }


  /**
   * Triggers the action to load the <i>_loadableBehaviorSubject</i>
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @protected
   * @param {string} url Target string to fetch the entity array (It's a game relative url)
   * @param {(result: E[]) => Promise<E[]>} [resultsCallback] Action to execute after fetching the results, MUST return the parsed result
   * @memberof GameBaseService
   */
  protected _loadSubject(url: string, resultsCallback?: (result: E[]) => Promise<E[]>): void {
    this.doGetWithAuthorizationToGame<E[]>(url).subscribe(async result => {
      const action = typeof resultsCallback === 'function'
        ? resultsCallback
        : async () => result;
      this._loadableBehaviorSubject.next(await action(result));
    });
  }
}
