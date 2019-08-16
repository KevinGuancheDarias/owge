import { Injectable } from '@angular/core';
import { CoreGameService } from '../../core/services/core-game.service';

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

  public constructor(private _coreGameService: CoreGameService) { }

  /**
   * Returns the entire ranking
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @returns
   * @memberof RankingService
   */
  public findAll() {
    return this._coreGameService.getWithAuthorizationToUniverse('ranking');
  }
}
