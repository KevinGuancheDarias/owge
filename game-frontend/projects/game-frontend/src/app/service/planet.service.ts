
import { Injectable } from '@angular/core';
import {filter} from 'rxjs/operators';
import { Observable ,  BehaviorSubject } from 'rxjs';

import { UniverseGameService } from '@owge/universe';

import { PlanetPojo } from './../shared-pojo/planet.pojo';
import { LoginSessionService } from '../login-session/login-session.service';

@Injectable()
export class PlanetService {

  /**
   * Planet list from the User
   *
   * @author Kevin Guanche Darias
   */
  public get myPlanets(): Observable<PlanetPojo[]> {
    return this._myPlanets.asObservable();
  }
  private _myPlanets: BehaviorSubject<PlanetPojo[]> = new BehaviorSubject(null);

  constructor(private _universeGameService: UniverseGameService, private _loginSessionService: LoginSessionService) {
    this.findMyPlanets();
  }

  public findMyPlanets(): Promise<void> {
    return new Promise(resolve => {
      this._universeGameService.getWithAuthorizationToUniverse('planet/findMyPlanets').subscribe(result => {
        this._myPlanets.next(result);
        resolve();
      });
    });
  }

  public isMine(planet: PlanetPojo): boolean {
    return planet.ownerId === this._loginSessionService.findTokenData().id;
  }

  public leavePlanet(planet: PlanetPojo): Observable<void> {
    return this._universeGameService.postwithAuthorizationToUniverse('planet/leave?planetId=' + planet.id);
  }

  public findSelectedPlanet(): Promise<PlanetPojo> {
    return new Promise(resolve => {
      this._loginSessionService.findSelectedPlanet.pipe(filter(planet => planet !== null)).subscribe(planet => resolve(planet));
    });
  }
}
