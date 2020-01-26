
import { Injectable } from '@angular/core';
import { filter } from 'rxjs/operators';
import { Observable, BehaviorSubject } from 'rxjs';

import { User, UserStorage } from '@owge/core';
import { UniverseGameService } from '@owge/universe';

import { Planet } from '../pojos/planet.pojo';
import { PlanetStore } from '../stores/planet.store';

@Injectable()
export class PlanetService {

  /**
   * Planet list from the User
   *
   * @author Kevin Guanche Darias
   */
  public get myPlanets(): Observable<Planet[]> {
    return this._myPlanets.asObservable();
  }
  private _myPlanets: BehaviorSubject<Planet[]> = new BehaviorSubject(null);

  private _user: User;

  constructor(
    private _universeGameService: UniverseGameService,
    private _userStorage: UserStorage<User>,
    private _planeStore: PlanetStore
  ) {
    this.findMyPlanets();
    this._userStorage.currentUser.subscribe(user => this._user = user);
  }


  /**
   * Defines the selected planet
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @param  planet
   */
  public defineSelectedPlanet(planet: Planet) {
    this._planeStore.selectedPlanet.next(planet);
  }

  public findMyPlanets(): Promise<Planet[]> {
    return new Promise(resolve => {
      this._universeGameService.getWithAuthorizationToUniverse('planet/findMyPlanets').subscribe(result => {
        this._myPlanets.next(result);
        resolve(result);
      });
    });
  }

  public isMine(planet: Planet): boolean {
    return planet.ownerId === this._user.id;
  }

  public leavePlanet(planet: Planet): Observable<void> {
    return this._universeGameService.postWithAuthorizationToUniverse('planet/leave?planetId=' + planet.id);
  }

  public findSelectedPlanet(): Promise<Planet> {
    return new Promise(resolve => {
      this._planeStore.selectedPlanet.pipe(filter(planet => planet !== null)).subscribe(planet => resolve(planet));
    });
  }
}
