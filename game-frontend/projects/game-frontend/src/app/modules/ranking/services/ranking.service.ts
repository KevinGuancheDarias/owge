import { Injectable } from '@angular/core';
import { UniverseGameService } from '@owge/universe';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @class RankingService
 */
@Injectable()
export class RankingService {

  public constructor(private _universeGameService: UniverseGameService) { }

  /**
   * Returns the entire ranking
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @returns
   * @memberof RankingService
   */
  public findAll() {
    return this._universeGameService.getWithAuthorizationToUniverse('ranking');
  }
}
