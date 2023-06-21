import { Injectable } from '@angular/core';

import { UniverseGameService, Planet } from '@owge/universe';
import { PlanetService } from '@owge/galaxy';

import { NavigationConfig } from '../shared/types/navigation-config.type';
import { NavigationData } from '../shared/types/navigation-data.type';
import { HttpParams } from '@angular/common/http';
import {filter, map, take} from 'rxjs/operators';
import {ActivatedRoute, Params, Router} from '@angular/router';
import {Observable} from 'rxjs';
import {LoggerHelper} from '@owge/core';

@Injectable()
export class NavigationService {

  private _lastNavigationPosition: NavigationConfig;
  private _selectedPlanet: Planet;
  private log: LoggerHelper = new LoggerHelper(this.constructor.name);

  constructor(
    private _universeGameService: UniverseGameService,
    private _planetService: PlanetService,
    private activatedRoute: ActivatedRoute,
    private router: Router
  ) {

  }

  /**
   * Finds the last navigated location, if has not navigated, will return the selected planet location
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @returns
   * @memberof NavigationService
   */
  public async findCurrentNavigationConfig(): Promise<NavigationConfig> {
    const urlNavigation = this.findNavigationConfigFromUrlSnapshot();
    if (!this._lastNavigationPosition && !urlNavigation) {
      this._selectedPlanet = await this._planetService.findCurrentPlanet().pipe(take(1)).toPromise();
      this._lastNavigationPosition = {
        galaxy: this._selectedPlanet.galaxyId,
        sector: this._selectedPlanet.sector,
        quadrant: this._selectedPlanet.quadrant
      };
    } else if(urlNavigation) {
      this._lastNavigationPosition = urlNavigation;
    }
    return this._lastNavigationPosition;
  }

  public findNavigationConfigFromUrl(): Observable<NavigationConfig> {
    return this.activatedRoute.queryParams
        .pipe(
            filter(queryParams => this.areValidQueryParams(queryParams)),
            map(queryParams => this.queryParamsToNavigationConfig(queryParams))
        );
  }

  public findNavigationConfigFromUrlSnapshot(): NavigationConfig {
    const queryParams = this.activatedRoute.snapshot.queryParams;
    return this.areValidQueryParams(queryParams)
        ? this.queryParamsToNavigationConfig(queryParams)
        : null;
  }

  public async navigate(targetPosition: NavigationConfig): Promise<NavigationData> {
    const navigationData = await this._universeGameService.requestWithAutorizationToContext('game', 'get', 'galaxy/navigate', null, {
      params: this._genUrlParams(targetPosition)
    }).toPromise();
    this._lastNavigationPosition = targetPosition;
    return navigationData;
  }

  public updateQueryParams(navigationConfig: NavigationConfig): Promise<boolean> {
    this.log.debug('Navigating to ', navigationConfig);
    navigationConfig.planetId = navigationConfig.planetId || null;
    return this.router.navigate(
      [],
    {
        relativeTo: this.activatedRoute,
        queryParams: navigationConfig,
        queryParamsHandling: 'merge'
      }
    );
  }

  private _genUrlParams(targetPosition: NavigationConfig): HttpParams {
    let retVal: HttpParams = new HttpParams();
    retVal = retVal.append('galaxyId', targetPosition.galaxy.toString());
    retVal = retVal.append('sector', targetPosition.sector.toString());
    retVal = retVal.append('quadrant', targetPosition.quadrant.toString());
    return retVal;
  }

  private queryParamsToNavigationConfig(queryParams: Params): NavigationConfig {
    return {
      galaxy: parseInt(queryParams.galaxy,10),
      sector: parseInt(queryParams.sector,10),
      quadrant: parseInt(queryParams.quadrant,10),
      planetId: queryParams.planetId ? parseInt(queryParams.planetId, 10) : null
    };
  }

  private areValidQueryParams(queryParams: Params): boolean {
    return queryParams.galaxy && queryParams.sector && queryParams.quadrant;
  }
}
