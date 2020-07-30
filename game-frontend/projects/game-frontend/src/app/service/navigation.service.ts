import { Injectable } from '@angular/core';

import { UniverseGameService, Planet } from '@owge/universe';
import { PlanetService } from '@owge/galaxy';

import { NavigationConfig } from '../shared/types/navigation-config.type';
import { NavigationData } from '../shared/types/navigation-data.type';
import { HttpParams } from '@angular/common/http';
import { take } from 'rxjs/operators';

@Injectable()
export class NavigationService {

  private _lastNavigationPosition: NavigationConfig;
  private _selectedPlanet: Planet;

  constructor(
    private _universeGameService: UniverseGameService,
    private _planetService: PlanetService,
  ) {

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
      this._selectedPlanet = await this._planetService.findCurrentPlanet().pipe(take(1)).toPromise();
      this._lastNavigationPosition = {
        galaxy: this._selectedPlanet.galaxyId,
        sector: this._selectedPlanet.sector,
        quadrant: this._selectedPlanet.quadrant
      };
    }
    return this._lastNavigationPosition;
  }

  public async navigate(targetPosition: NavigationConfig): Promise<NavigationData> {
    const navigationData = await this._universeGameService.requestWithAutorizationToContext('game', 'get', 'galaxy/navigate', null, {
      params: this._genUrlParams(targetPosition)
    }).toPromise();
    this._lastNavigationPosition = targetPosition;
    return navigationData;
  }

  private _genUrlParams(targetPosition: NavigationConfig): HttpParams {
    let retVal: HttpParams = new HttpParams();
    retVal = retVal.append('galaxyId', targetPosition.galaxy.toString());
    retVal = retVal.append('sector', targetPosition.sector.toString());
    retVal = retVal.append('quadrant', targetPosition.quadrant.toString());
    return retVal;
  }
}
