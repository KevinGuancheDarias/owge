import { Injectable } from '@angular/core';
import { URLSearchParams } from '@angular/http';
import { Observable } from 'rxjs/Observable';

import { ProgrammingError } from '../../error/programming.error';
import { NavigationConfig } from '../shared/types/navigation-config.type';
import { GameBaseService } from './game-base.service';
import { NavigationData } from '../shared/types/navigation-data.type';

@Injectable()
export class NavigationService extends GameBaseService {

  private _lastNavigationPosition: NavigationConfig;

  constructor() {
    super();
    this._requireSelectedPlanet();
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
    const navigationData = await this.doGetWithAuthorizationToGame('galaxy/navigate', this._genUrlParams(targetPosition)).toPromise();
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
      await this._findSelectedPlanet();
      if (!this._selectedPlanet) {
        throw new ProgrammingError('Selected planet is undefined, SHOULD NEVER, NEVER happend');
      }
    }

  }

  private _genUrlParams(targetPosition: NavigationConfig): URLSearchParams {
    const retVal: URLSearchParams = new URLSearchParams();
    retVal.append('galaxyId', targetPosition.galaxy.toString());
    retVal.append('sector', targetPosition.sector.toString());
    retVal.append('quadrant', targetPosition.quadrant.toString());
    return retVal;
  }
}
