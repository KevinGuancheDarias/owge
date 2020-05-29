
import { Injectable } from '@angular/core';
import { filter } from 'rxjs/operators';
import { Observable, BehaviorSubject } from 'rxjs';

import { User, UserStorage, AbstractWebsocketApplicationHandler, SessionStore } from '@owge/core';
import { UniverseGameService } from '@owge/universe';

import { Planet } from '../pojos/planet.pojo';
import { PlanetStore } from '../stores/planet.store';

@Injectable()
export class PlanetService extends AbstractWebsocketApplicationHandler {
  private _user: User;
  private _currentPlanet: Planet;
  private _planeStore: PlanetStore;

  constructor(
    private _universeGameService: UniverseGameService,
    private _userStorage: UserStorage<User>,
    _sessionStore: SessionStore,
  ) {
    super();
    this._eventsMap = {
      planet_owned_change: '_onPlanetOwnedChange'
    };
    this._planeStore = new PlanetStore(_sessionStore);
    this._userStorage.currentUser.subscribe(user => this._user = user);
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returns
   */
  public async workaroundSync(): Promise<void> {
    this._onPlanetOwnedChange(
      await this._universeGameService.requestWithAutorizationToContext('game', 'get', 'planet/findMyPlanets').toPromise()
    );
  }

  public findMyPlanets(): Observable<Planet[]> {
    return this._planeStore.ownedPlanetList.asObservable();
  }

  /**
   * Defines the selected planet
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @param  planet
   */
  public defineSelectedPlanet(planet: Planet) {
    this._currentPlanet = planet;
    this._planeStore.selectedPlanet.next(planet);
  }

  public isMine(planet: Planet): boolean {
    return planet.ownerId === this._user.id;
  }

  public leavePlanet(planet: Planet): Observable<void> {
    return this._universeGameService.requestWithAutorizationToContext('game', 'post', 'planet/leave?planetId=' + planet.id, 'not used');
  }

  /**
   *
   * @deprecated As of 0.9.0 it's better to use an observable as we want reactive supper powers! :)
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @returns
   */
  public async findSelectedPlanet(): Promise<Planet> {
    this._log.warnDeprecated('findSelectedPlanet', '0.9.0', 'findCurrentPlanet');
    return this._currentPlanet;
  }

  /**
   * Finds the current planet as an observable
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returns {Observable<Planet>}
   */
  public findCurrentPlanet(): Observable<Planet> {
    return this._planeStore.selectedPlanet.asObservable();
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @protected
   * @param content
   */
  protected _onPlanetOwnedChange(content: Planet[]): void {
    if (!this._currentPlanet) {
      this._currentPlanet = content.find(current => current.home);
      this.defineSelectedPlanet(this._currentPlanet);
    }
    if (!content.some(current => current.id === this._currentPlanet.id)) {
      const home: Planet = content.find(current => current.home);
      this.defineSelectedPlanet(home);
    }
    this._planeStore.ownedPlanetList.next(content);
  }
}
