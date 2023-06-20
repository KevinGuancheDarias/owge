
import { map } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import {Observable, Subject} from 'rxjs';

import { SessionService } from './session.service';
import { OwgeCoreConfig } from '../pojos/owge-core-config';
import { CoreHttpService } from './core-http.service';
import { OwgeUserModule } from '../owge-user.module';
import { ToastrService } from './toastr.service';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable({ providedIn: OwgeUserModule })
export class LoginService {
    get onLogin(): Observable<void> {
        return this.onLoginEmitter.asObservable();
    }

    private onLoginEmitter: Subject<void> = new Subject();

  constructor(
    private _coreHttpService: CoreHttpService,
    private _sessionService: SessionService,
    private _accountConfig: OwgeCoreConfig,
    private _toastrService: ToastrService
  ) { }

  /**
   * Executes the game action agains the OWGE Authentication server
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param  email
   * @param password
   * @returns JWT token
   * @memberof LoginService
   */
  public login(email: string, password: string): Observable<string> {
    const params: HttpParams = new HttpParams(
      {
        fromObject:
        {
          grant_type: 'password',
          client_id: this._accountConfig.loginClientId,
          client_secret: this._accountConfig.loginClientSecret,
          username: email,
          password
        }
      }
    );
    const errorHandler = this._toastrService.handleHttpError.bind(this._toastrService);
    return this._coreHttpService.post(`${this._accountConfig.url}/${this._accountConfig.loginEndpoint}`, params, { errorHandler }).pipe(
      map(res => {
        this._sessionService.setTokenPojo(res.token);
        this.onLoginEmitter.next();
        return this._sessionService.getRawToken();
      }));
  }
}
