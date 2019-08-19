
import {map} from 'rxjs/operators';
import { Injectable } from '@angular/core';
import {  HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Config, CoreHttpService } from '@owge/core';
import { environment } from '../../environments/environment';
import { LoginSessionService } from '../login-session/login-session.service';

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

    return this._coreHttpService.post(`${Config.accountServerUrl}${Config.accountLoginendpoint}`, params).pipe(
      map(res => {
        this._loginSessionService.setTokenPojo(res.token);
        return this._loginSessionService.getRawToken();
      }));
  }

}
