import { GameBaseService } from './game-base.service';
import { PlanetPojo } from './../shared-pojo/planet.pojo';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

@Injectable()
export class PlanetService extends GameBaseService {

  /**
   * Planet list from the User
   *
   * @author Kevin Guanche Darias
   */
  public get myPlanets(): Observable<PlanetPojo[]> {
    return this._myPlanets.asObservable();
  }
  private _myPlanets: BehaviorSubject<PlanetPojo[]> = new BehaviorSubject(null);

  constructor() {
    super();
    this.findMyPlanets();
  }

  public findMyPlanets(): Promise<void> {
    return new Promise(resolve => {
      this.doGetWithAuthorizationToGame('planet/findMyPlanets').subscribe(result => {
        this._myPlanets.next(result);
        resolve();
      });
    });
  }

  public isMine(planet: PlanetPojo): boolean {
    return planet.ownerId === this._loginSessionService.findTokenData().id;
  }

  public leavePlanet(planet: PlanetPojo): Observable<void> {
    return this._doPostWithAuthorizationToGame('planet/leave?planetId=' + planet.id, {});
  }
}
