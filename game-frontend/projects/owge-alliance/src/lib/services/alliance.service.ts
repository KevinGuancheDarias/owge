import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { switchMap ,  first ,  map } from 'rxjs/operators';

import { AllianceStorage } from '../storages/alliance.storage';
import { UserStorage } from '../../user/storages/user.storage';
import { Alliance } from '../types/alliance.type';
import { CoreGameService } from '../../core/services/core-game.service';
import { User } from '../../user/types/user.type';
import { AllianceJoinRequest } from '../types/alliance-join-request.type';

/**
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @class AllianceService
 */
@Injectable()
export class AllianceService {

  constructor(private _allianceStorage: AllianceStorage, private _userStorage: UserStorage, private _coreGameService: CoreGameService) {
    this._loadMyAlliance();
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @returns {Observable<Alliance[]>}
   * @memberof AllianceService
   */
  public findAll(): Observable<Alliance[]> {
    return this._coreGameService.getWithAuthorizationToUniverse('alliance');
  }

  /**
   * Find all members for given alliance
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param {number} allianceId
   * @returns {Observable<User[]>}
   * @memberof AllianceService
   */
  public findMembers(allianceId: number): Observable<User[]> {
    return this._coreGameService.getWithAuthorizationToUniverse(`alliance/${allianceId}/members`);
  }

  /**
   * Saves an alliance
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param {Alliance} alliance
   * @returns {Observable<Alliance>} Saved alliance in the backend
   * @memberof AllianceService
   */
  public save(alliance: Alliance): Observable<Alliance> {
    const retVal: Observable<Alliance> = alliance.id
      ? this._coreGameService.putwithAuthorizationToUniverse('alliance', alliance)
      : this._coreGameService.postwithAuthorizationToUniverse('alliance', alliance);
    return retVal.pipe(
      switchMap(saved => this._updateStorages(saved))
    );
  }

  /**
   * Deletes an alliance
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param {number} id
   * @returns {Observable<Alliance>} Should always return null
   * @memberof AllianceService
   */
  public delete(id: number): Observable<Alliance> {
    return this._coreGameService.deleteWithAuthorizationToUniverse('alliance').pipe(
      switchMap(deleted => this._updateStorages(deleted))
    );
  }

  /**
   * Sends a new join request
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param {number} allianceId
   * @returns {Observable<AllianceJoinRequest>}
   * @memberof AllianceService
   */
  public requestJoin(allianceId: number): Observable<AllianceJoinRequest> {
    return this._coreGameService.postwithAuthorizationToUniverse('alliance/requestJoin', { allianceId });
  }

  /**
   * Finds all the join request for <b>my</b> alliance
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @returns {Observable<AllianceJoinRequest[]>}
   * @memberof AllianceService
   */
  public findJoinRequest(): Observable<AllianceJoinRequest[]> {
    return this._coreGameService.getWithAuthorizationToUniverse('alliance/listRequest');
  }


  /**
   *  Accepts a join request
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param {number} joinRequestId
   * @returns {Observable<void>}
   * @memberof AllianceService
   */
  public acceptJoinRequest(joinRequestId: number): Observable<void> {
    return this._coreGameService.postwithAuthorizationToUniverse('alliance/acceptJoinRequest', { joinRequestId });
  }

  /**
   * Rejects a join request
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param {number} joinRequestId
   * @returns {Observable<void>}
   * @memberof AllianceService
   */
  public rejectJoinRequest(joinRequestId: number): Observable<void> {
    return this._coreGameService.postwithAuthorizationToUniverse('alliance/rejectJoinRequest', { joinRequestId });
  }


  /**
   * Leaves the alliance
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @returns {Observable<Alliance>}
   * @memberof AllianceService
   */
  public leave(): Observable<Alliance> {
    return this._coreGameService.postwithAuthorizationToUniverse('alliance/leave').pipe(
      switchMap(_ => this._updateStorages(null))
    );
  }

  private async _loadMyAlliance(): Promise<void> {
    this._userStorage.currentUser.subscribe(user =>
      this._allianceStorage.userAlliance.next(user.alliance ? user.alliance : null));
  }

  private _updateStorages(alliance: Alliance): Observable<Alliance> {
    return this._userStorage.currentUser.pipe(
      first(),
      map(user => {
        user.alliance = alliance;
        this._userStorage.currentUser.next(user);
        this._allianceStorage.userAlliance.next(alliance);
        return alliance;
      })
    );
  }
}
