import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { isEqual } from 'lodash-es';

import { User, AbstractWebsocketApplicationHandler, SessionStore, StorageOfflineHelper } from '@owge/core';
import { UniverseGameService, WsEventCacheService, UniverseCacheManagerService, UserStorage } from '@owge/universe';

import { Planet } from '@owge/universe';
import { PlanetStore } from '../stores/planet.store';
import { distinctUntilChanged } from 'rxjs/operators';

@Injectable()
export class PlanetService extends AbstractWebsocketApplicationHandler {
  private _user: User;
  private _currentPlanet: Planet;
  private _planeStore: PlanetStore;
  private _offlinePlanetOwnedChange: StorageOfflineHelper<Planet[]>;

  constructor(
    private _universeGameService: UniverseGameService,
    private _userStorage: UserStorage<User>,
    private _wsEventCacheService: WsEventCacheService,
    private _universeCacheManagerService: UniverseCacheManagerService,
    sessionStore: SessionStore
  ) {
    super();
    this._eventsMap = {
      planet_owned_change: '_onPlanetOwnedChange',
      planet_explored_event: '_onPlanetExploredEvent'
    };
    this._planeStore = new PlanetStore(sessionStore);
    this._userStorage.currentUser.subscribe(user => this._user = user);
  }

  public async createStores(): Promise<void> {
    this._offlinePlanetOwnedChange = this._universeCacheManagerService.getStore('planet.owned_list');
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returns
   */
  public async workaroundSync(): Promise<void> {
    this._onPlanetOwnedChange(await this._wsEventCacheService.findFromCacheOrRun('planet_owned_change', this._offlinePlanetOwnedChange,
      async () => await this._universeGameService.requestWithAutorizationToContext('game', 'get', 'planet/findMyPlanets').toPromise()
    ));
    if (this._wsEventCacheService.hasChanged('planet_explored_event')) {
      this._onPlanetExploredEvent(null);
    }
  }

  public async workaroundInitialOffline(): Promise<void> {
    await this._offlinePlanetOwnedChange.doIfNotNull(content => this._onPlanetOwnedChange(content));
  }

  public findMyPlanets(): Observable<Planet[]> {
    return this._planeStore.ownedPlanetList.asObservable().pipe(distinctUntilChanged(isEqual));
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
   * @returns
   */
  public findCurrentPlanet(): Observable<Planet> {
    return this._planeStore.selectedPlanet.asObservable().pipe(distinctUntilChanged((a, b) => a.id === b.id));
  }

  /**
   *
   * Fires when the ws emits an explored event, or <b>with null</b> if the socket was offline, but a value was emitted
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returnss
   */
  public onPlanetExplored(): Observable<Planet> {
    return this._planeStore.exploredEvent.asObservable();
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @protected
   * @param content
   */
  protected _onPlanetOwnedChange(content: Planet[]): void {
    if (!this._isCachePanic(content)) {
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

  protected _onPlanetExploredEvent(content: Planet): void {
    this._planeStore.exploredEvent.next(content);
  }
}
