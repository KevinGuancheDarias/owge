import { Injectable } from '@angular/core';
import { Response, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Observable';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/catch';
import 'rxjs/add/observable/throw';

import { GameBaseService } from './../service/game-base.service';
import { Config } from '../config/config.pojo';
import { environment } from '../../environments/environment';

@Injectable()
export class LoginService extends GameBaseService {
  constructor() {
    super();
  }


  /**
   * Executes the game action agains the OWGE Authentication server
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {string} email
   * @param {string} password
   * @returns {Observable<string>} JWT token
   * @memberof LoginService
   */
  public login(email: string, password: string): Observable<string> {
    const requestOptions: RequestOptions = new RequestOptions();
    requestOptions.headers = Config.genCommonFormUrlencoded();
    const params: URLSearchParams = new URLSearchParams(<any>{
      grant_type: 'password',
      client_id: environment.loginClientId,
      client_secret: environment.loginClientSecret,
      username: email,
      password
    });

    return this.http.post(`${Config.ACCOUNT_SERVER_URL}oauth/token`, params.toString(), requestOptions)
      .map((res: Response) => {
        this.getLoginSessionService().setTokenPojo(res.json().token);
        return this.getLoginSessionService().getRawToken();
      })
      .catch((error: Response) => {
        return this.handleError(error);
      });
  }

}
