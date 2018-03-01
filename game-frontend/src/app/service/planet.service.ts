import { GameBaseService } from './game-base.service';
import { PlanetPojo } from './../shared-pojo/planet.pojo';
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable()
export class PlanetService extends GameBaseService {

  /**
   * Planet list from the User
   *
   * @author Kevin Guanche Darias
   */
  public get myPlanets(): BehaviorSubject<PlanetPojo[]> {
    return this._myPlanets;
  }
  private _myPlanets: BehaviorSubject<PlanetPojo[]> = new BehaviorSubject(null);

  constructor() {
    super();
    this.findMyPlanets();
  }

  public findMyPlanets(): void {
    this.doGetWithAuthorizationToGame('planet/findMyPlanets').subscribe(result => this._myPlanets.next(result));
  }
}
