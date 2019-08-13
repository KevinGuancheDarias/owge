import { Injectable } from '@angular/core';

import { ProgrammingError } from '../../error/programming.error';
import { NavigationConfig } from '../shared/types/navigation-config.type';
import { NavigationData } from '../shared/types/navigation-data.type';
import { LoginSessionService } from '../login-session/login-session.service';
import { PlanetPojo } from '../shared-pojo/planet.pojo';
import { CoreGameService } from '../modules/core/services/core-game.service';
import { HttpParams } from '@angular/common/http';
import { PlanetService } from './planet.service';

@Injectable()
export class NavigationService {

  private _lastNavigationPosition: NavigationConfig;
  private _selectedPlanet: PlanetPojo;

  constructor(
    private _loginSessionService: LoginSessionService,
    private _coreGameService: CoreGameService,
    private planetService: PlanetService) {
    this._loginSessionService.findSelectedPlanet.subscribe(selectedPlanet => this._selectedPlanet = selectedPlanet);
  }

  /**
   * Finds the last navigated location, if has not navigated, will return the selected planet location
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @returns {NavigationConfig}
   * @memberof NavigationService
   */
  public async findCurrentNavigationConfig(): Promise<NavigationConfig> {
    if (!this._lastNavigationPosition) {
      await this._checkSelectedPlanet();
      this._lastNavigationPosition = {
        galaxy: this._selectedPlanet.galaxyId,
        sector: this._selectedPlanet.sector,
        quadrant: this._selectedPlanet.quadrant
      };
    }
    return this._lastNavigationPosition;
  }

  public async navigate(targetPosition: NavigationConfig): Promise<NavigationData> {
    const navigationData = await this._coreGameService.getWithAuthorizationToUniverse('galaxy/navigate', {
      params: this._genUrlParams(targetPosition)
    }).toPromise();
    this._lastNavigationPosition = targetPosition;
    return navigationData;
  }

  /**
   * Check if the <b>_selectedPlanet</b> property is defined
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @throws {ProgrammingError} If not defined
   * @private
   * @memberof NavigationService
   */
  private async _checkSelectedPlanet(): Promise<void> {
    if (!this._selectedPlanet) {
      await this.planetService.findSelectedPlanet();
      if (!this._selectedPlanet) {
        throw new ProgrammingError('Selected planet is undefined, SHOULD NEVER, NEVER happend');
      }
    }

  }

  private _genUrlParams(targetPosition: NavigationConfig): HttpParams {
    let retVal: HttpParams = new HttpParams();
    retVal = retVal.append('galaxyId', targetPosition.galaxy.toString());
    retVal = retVal.append('sector', targetPosition.sector.toString());
    retVal =  retVal.append('quadrant', targetPosition.quadrant.toString());
    return retVal;
  }
}
