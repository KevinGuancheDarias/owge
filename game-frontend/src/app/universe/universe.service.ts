import { GameBaseService } from './../service/game-base.service';
import { Injectable } from '@angular/core';
import { Config } from '../config/config.pojo';
import { Observable } from 'rxjs/Observable';
import { URLSearchParams } from '@angular/http';

import { Universe } from '../shared-pojo/universe.pojo';

@Injectable()
export class UniverseService extends GameBaseService {

  constructor() {
    super();
  }

  public findOfficials(): Observable<Universe[]> {
    return this.doGet(Config.ACCOUNT_SERVER_URL + 'universe/findOfficials');
  }

  /**
   * Will check if the logged in user exists in that universe
   *
   * @param {string} token - The Raw JWT token
   * @author Kevin Guanche Darias
   */
  public userExists(): Observable<boolean> {
    return this.doGetWithAuthorization(
      this.getLoginSessionService().getSelectedUniverse().restBaseUrl + '/user/exists'
    );
  }

  /**
   * Subscribes the user to the universe <br />
   * The server returns a boolean, if false it means the user was already registered, which is completely unexpected behavior
   *
   * @param {number} factionId The id of the faction to use for the subscription proccess
   * @author Kevin Guanche Darias
   */
  public subscribe(factionId: number): Observable<boolean> {
    const params: URLSearchParams = new URLSearchParams();
    params.append('factionId', factionId.toString());
    return this.doGetWithAuthorization(
      this.getLoginSessionService().getSelectedUniverse().restBaseUrl + '/user/subscribe', params
    );
  }
}
