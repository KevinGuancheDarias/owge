import { GameBaseService } from './../service/game-base.service';
import { Injectable } from '@angular/core';
import { Response, RequestOptions } from '@angular/http';
import { Config } from '../config/config.pojo';
import { Observable } from 'rxjs/Observable';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/catch';

@Injectable()
export class LoginService extends GameBaseService {
  constructor() {
    super();
  }


  /**
   * Executes the game action agains the SGT Authentication server
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {string} email
   * @param {string} password
   * @returns {Observable<string>} JWT token
   * @memberof LoginService
   */
  public login(email: string, password: string): Observable<string> {
    const urlEncodedParams = 'email=' + encodeURIComponent(email) + '&password=' + password;
    const requestOptions: RequestOptions = new RequestOptions();
    requestOptions.headers = Config.genCommonFormUrlencoded();

    return this.http.post(Config.ACCOUNT_SERVER_URL + 'login', urlEncodedParams, requestOptions)
      .map((res: Response) => {
        this.getLoginSessionService().setTokenPojo(res.json().token);
        return this.getLoginSessionService().getRawToken();
      })
      .catch((error: Response) => {
        return this.handleError(error);
      });
  }

}
