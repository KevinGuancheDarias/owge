import { Component, OnInit } from '@angular/core';
import { AllianceJoinRequest } from '../../types/alliance-join-request.type';
import { AllianceService } from '../../services/alliance.service';
import { UserStorage } from '@owge/universe';
import { UserWithAlliance } from '../../types/user-with-alliance.type';

/**
 * List and handles join request
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
@Component({
  selector: 'owge-alliance-list-join-request',
  templateUrl: './list-join-request.component.html',
  styleUrls: ['./list-join-request.component.less']
})
export class ListJoinRequestComponent implements OnInit {

  public isAllianceOwner: boolean = null;
  public joinRequests: AllianceJoinRequest[];

  public constructor(private _userStorage: UserStorage<UserWithAlliance>, private _allianceService: AllianceService) {

  }

  public ngOnInit() {
    this._userStorage.currentUser.subscribe(async user => {
      this.isAllianceOwner = user.alliance && user.alliance.owner === user.id;
      if (this.isAllianceOwner) {
        this._loadJoinRequest();
      }
    });
  }

  /**
   * Accepts a request
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param  request
   * @returns
   */
  public async acceptRequest(request: AllianceJoinRequest): Promise<void> {
    await this._allianceService.acceptJoinRequest(request.id).toPromise();
    await this._loadJoinRequest();
  }

  /**
   * Rejects a request
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param request
   * @returns
   */
  public async rejectRequest(request: AllianceJoinRequest): Promise<void> {
    await this._allianceService.rejectJoinRequest(request.id).toPromise();
    await this._loadJoinRequest();
  }

  private async _loadJoinRequest(): Promise<void> {
    this.joinRequests = await this._allianceService.findJoinRequest().toPromise();
  }
}
