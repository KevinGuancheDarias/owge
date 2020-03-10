import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { switchMap, first, map, take, tap } from 'rxjs/operators';

import { UserStorage } from '@owge/core';
import { UniverseGameService } from '@owge/universe';

import { AllianceStorage } from '../storages/alliance.storage';
import { Alliance } from '../types/alliance.type';
import { AllianceJoinRequest } from '../types/alliance-join-request.type';
import { UserWithAlliance } from '../types/user-with-alliance.type';

/**
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
@Injectable()
export class AllianceService {
  private _myJoinRequests: AllianceJoinRequest[];

  constructor(
    private _allianceStorage: AllianceStorage,
    private _userStorage: UserStorage<UserWithAlliance>,
    private _universeGameService: UniverseGameService) {
    this._loadMyAlliance();
    this._loadMyJoinQueries();
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @returns
   */
  public findAll(): Observable<Alliance[]> {
    return this._universeGameService.getWithAuthorizationToUniverse('alliance');
  }

  /**
   * Find all members for given alliance
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param allianceId
   * @returns
   */
  public findMembers(allianceId: number): Observable<UserWithAlliance[]> {
    return this._universeGameService.getWithAuthorizationToUniverse(`alliance/${allianceId}/members`);
  }

  /**
   * Saves an alliance
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param alliance
   * @returns Saved alliance in the backend
   */
  public save(alliance: Alliance): Observable<Alliance> {
    let safeAlliance: Alliance;
    if (alliance.owner) {
      safeAlliance = { ...alliance };
      delete safeAlliance.owner;
    } else {
      safeAlliance = alliance;
    }
    const retVal: Observable<Alliance> = alliance.id
      ? this._universeGameService.putwithAuthorizationToUniverse('alliance', safeAlliance)
      : this._universeGameService.postWithAuthorizationToUniverse('alliance', safeAlliance);
    return retVal.pipe(
      switchMap(saved => this._updateStorages(saved))
    );
  }

  /**
   * Deletes an alliance
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param id
   * @returns Should always return null
   */
  public delete(id: number): Observable<Alliance> {
    return this._universeGameService.deleteWithAuthorizationToUniverse('alliance').pipe(
      switchMap(deleted => this._updateStorages(deleted))
    );
  }

  /**
   * Sends a new join request
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param allianceId
   * @returns
   */
  public requestJoin(allianceId: number): Observable<AllianceJoinRequest> {
    return this._universeGameService.postWithAuthorizationToUniverse('alliance/requestJoin', { allianceId })
      .pipe(tap(val => {
        this._myJoinRequests.push(val);
        this._allianceStorage.userJoinRequests.next(this._myJoinRequests);
      }));
  }

  /**
   * Finds all the join request for <b>my</b> alliance
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @returns
   */
  public findJoinRequest(): Observable<AllianceJoinRequest[]> {
    return this._universeGameService.getWithAuthorizationToUniverse('alliance/listRequest');
  }


  /**
   *  Accepts a join request
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param joinRequestId
   * @returns
   */
  public acceptJoinRequest(joinRequestId: number): Observable<void> {
    return this._universeGameService.postWithAuthorizationToUniverse('alliance/acceptJoinRequest', { joinRequestId });
  }

  /**
   * Rejects a join request
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param joinRequestId
   * @returns
   */
  public rejectJoinRequest(joinRequestId: number): Observable<void> {
    return this._universeGameService.postWithAuthorizationToUniverse('alliance/rejectJoinRequest', { joinRequestId });
  }

  /**
   * Leaves the alliance
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @returns
   */
  public leave(): Observable<Alliance> {
    return this._universeGameService.postWithAuthorizationToUniverse('alliance/leave').pipe(
      switchMap(_ => this._updateStorages(null))
    );
  }

  /**
   * The user join alliance requests
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.1
   * @returns
   */
  public findMyRequests(): Observable<AllianceJoinRequest[]> {
    return this._allianceStorage.userJoinRequests.asObservable();
  }

  /**
   * Cancels the user emmited join request
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.1
   * @param joinRequest
   */
  public cancelMyRequests(joinRequest: AllianceJoinRequest): Observable<void> {
    return this._universeGameService.requestWithAutorizationToContext('game', 'delete', `alliance/my-requests/${joinRequest.id}`)
      .pipe(tap(() => {
        this._myJoinRequests = this._myJoinRequests.filter(current => current.id !== joinRequest.id);
        this._allianceStorage.userJoinRequests.next(this._myJoinRequests);
      }));
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

  private _loadMyJoinQueries(): void {
    this._universeGameService.requestWithAutorizationToContext('game', 'get', 'alliance/my-requests')
      .pipe(take(1))
      .subscribe(val => {
        this._allianceStorage.userJoinRequests.next(val);
        this._myJoinRequests = val;
      });
  }
}
