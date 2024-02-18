import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Config, ProgrammingError, CoreHttpService, LoggerHelper } from '@owge/core';
import { UniverseGameService } from '@owge/universe';

import { UniverseLocalConfig } from '../shared/types/universe-local-config.type';
import { HttpParams } from '@angular/common/http';
import { LoginSessionService } from '../login-session/login-session.service';
import { Universe } from '@owge/types/universe';

@Injectable()
export class UniverseService {
  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);

  constructor(
    private _coreHttpService: CoreHttpService,
    private _universeGameService: UniverseGameService,
    private _loginSessionService: LoginSessionService
  ) { }

  /**
   *
   *
   * @deprecated As of 0.8.0 it's better to use the version in the OwgeUniverse lib
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.3.0
   * @returns
   * @memberof UniverseService
   */
  public findOfficials(): Observable<Universe[]> {
    this._log.warnDeprecated('UniverseService.findOfficials', '0.8.0', 'OwgeUniverse/UniverseService.findOfficials');
    return this._coreHttpService.get(Config.accountServerUrl + '/universe/findOfficials');
  }

  /**
   * Will check if the logged in user exists in that universe
   *
   * @param token - The Raw JWT token
   * @author Kevin Guanche Darias
   */
  public userExists(): Observable<boolean> {
    return this._universeGameService.getWithAuthorizationToUniverse('user/exists');
  }

  /**
   * Subscribes the user to the universe <br />
   * The server returns a boolean, if false it means the user was already registered, which is completely unexpected behavior
   *
   * @param factionId The id of the faction to use for the subscription proccess
   * @author Kevin Guanche Darias
   */
  public subscribe(factionId: number): Observable<boolean> {
    let params: HttpParams = new HttpParams();
    params = params.append('factionId', factionId.toString());
    return this._universeGameService.getWithAuthorizationToUniverse('user/subscribe', { params });
  }

  public findUniverseUserLocalConfig(): UniverseLocalConfig {
    const config = localStorage.getItem(this.findUniverseLocalStorageKey());
    return config
      ? JSON.parse(config)
      : {};
  }

  public saveUniverseLocalConfig(config: UniverseLocalConfig): void {
    localStorage.setItem(this.findUniverseLocalStorageKey(), JSON.stringify(config));
  }


  /**
   * Returns true if the running version is more recent than the last used by the user in their last access
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param currentVersion
   * @returns
   * @memberof UniverseService
   */
  public isUpdatedVersion(currentVersion: string): boolean {
    const config: UniverseLocalConfig = this.findUniverseUserLocalConfig();
    const notifiedVersionNumber: number = this.findVersionAsNumber(config.notifiedVersion);
    const currentVersionAsNumber: number = this.findVersionAsNumber(currentVersion);
    return currentVersionAsNumber > notifiedVersionNumber;
  }

  private findUniverseLocalStorageKey(): string {
    const { id, name } = this._loginSessionService.getSelectedUniverse();
    return `U${id}___${name.replace(/ /g, '_')}`;
  }


  /**
   * Finds the numeric representation of the version string
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @private
   * @param version Input version, should look like <b>0.3.0</b>, if falsy will return 0
   * @returns
   * @throws {ProgrammingError} When passed version string is not valid
   * @memberof UniverseService
   */
  private findVersionAsNumber(version: string): number {
    if (!version) {
      return 0;
    } else {
      const versionSeparator = 100;
      if (!version.match(/^[0-9]{1,4}\.[0-9]{1,4}\.[0-9]{1,4}$/)) {
        throw new ProgrammingError('Passed version string is not valid');
      }
      const [major, minor, patch] = version.split('.').map(current => parseInt(current, 10));
      return major * (versionSeparator ** 3) + (minor * (versionSeparator ** 2)) + (patch * versionSeparator);
    }
  }
}
