import { Component, OnInit, Input, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { UserStorage, LoadingService } from '@owge/core';
import { WidgetConfirmationDialogComponent } from '@owge/widgets';

import { Alliance } from '../../types/alliance.type';
import { AllianceService } from '../../services/alliance.service';
import { AllianceJoinRequest } from '../../types/alliance-join-request.type';
import { UserWithAlliance } from '../../types/user-with-alliance.type';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
@Component({
  selector: 'owge-alliance-details',
  templateUrl: './alliance-details.component.html',
  styleUrls: ['./alliance-details.component.less']
})
export class AllianceDetailsComponent implements OnInit {

  /**
   * Alliance to display info from
   *
   * @since 0.7.0
   */
  @Input() public alliance: Alliance;
  @ViewChild('confirmDialog', { static: true }) public confirmDialog: WidgetConfirmationDialogComponent;

  public members: UserWithAlliance[];
  public currentUser: UserWithAlliance;
  public vConfirmDeleteText: string;

  public constructor(
    private _allianceService: AllianceService,
    private _userStorage: UserStorage<UserWithAlliance>,
    private _translateService: TranslateService,
    private _loadingService: LoadingService,
    private _router: Router
  ) { }

  async ngOnInit() {
    this._userStorage.currentUser.subscribe(currentUser => this.currentUser = currentUser);
    this.members = await this._allianceService.findMembers(this.alliance.id).toPromise();
  }

  /**
   * Deletes user owned alliance
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @returns
   */
  public async clickDelete(): Promise<void> {
    this.vConfirmDeleteText = await this._translateService.get(
      'ALLIANCE.DELETE_CONFIRMATION',
      { allianceName: this.alliance.name }
    ).toPromise();
    this.confirmDialog.show();

  }

  /**
   * Issues a new join request
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @returns
   */
  public clickRequestJoin(): Promise<AllianceJoinRequest> {
    return this._allianceService.requestJoin(this.alliance.id).toPromise();
  }

  /**
   * Leaves an alliance
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   */
  public clickLeave(): void {
    this._loadingService.addPromise(this._allianceService.leave().toPromise());
  }

  /**
   * Executed when the deletation has been confirmed or rejected
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param result
   */
  public async onDeleteConfirm(result: boolean): Promise<void> {
    if (result) {
      await this._loadingService.addPromise(this._allianceService.delete(this.alliance.id).toPromise());
      this.confirmDialog.hide();
      this._router.navigate(['/alliance/browse']);
    }
  }
}
