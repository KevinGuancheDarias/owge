import { Injectable } from '@angular/core';
import {  HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/catch';
import 'rxjs/add/observable/throw';

import { Config } from '../config/config.pojo';
import { environment } from '../../environments/environment';
import { LoginSessionService } from '../login-session/login-session.service';
import { CoreHttpService } from '../modules/core/services/core-http.service';

@Injectable()
export class LoginService {
  constructor(private _coreHttpService: CoreHttpService, private _loginSessionService: LoginSessionService) { }

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
    const params: HttpParams = new HttpParams(
      { fromObject :
        {
          grant_type: 'password',
          client_id: environment.loginClientId,
          client_secret: environment.loginClientSecret,
          username: email,
          password
        }
      }
    );

    return this._coreHttpService.post(`${Config.ACCOUNT_SERVER_URL}${Config.ACCOUNT_LOGIN_ENDPOINT}`, params)
      .map(res => {
        this._loginSessionService.setTokenPojo(res.token);
        return this._loginSessionService.getRawToken();
      });
  }

}
