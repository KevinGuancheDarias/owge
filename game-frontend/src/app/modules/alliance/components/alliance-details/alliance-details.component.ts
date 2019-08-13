import { Component, OnInit, Input, ViewChild } from '@angular/core';
import { Alliance } from '../../types/alliance.type';
import { User } from '../../../user/types/user.type';
import { AllianceService } from '../../services/alliance.service';
import { UserStorage } from '../../../user/storages/user.storage';
// tslint:disable-next-line:max-line-length
import { WidgetConfirmationDialogComponent } from '../../../widgets/components/widget-confirmation-dialog/widget-confirmation-dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { LoadingService } from '../../../../services/loading.service';
import { AllianceJoinRequest } from '../../types/alliance-join-request.type';
import { Router } from '@angular/router';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @class AllianceDetailsComponent
 * @implements {OnInit}
 */
@Component({
  selector: 'app-alliance-details',
  templateUrl: './alliance-details.component.html',
  styleUrls: ['./alliance-details.component.less']
})
export class AllianceDetailsComponent implements OnInit {

  /**
   * Alliance to display info from
   *
   * @since 0.7.0
   * @type {Alliance}
   * @memberof AllianceDetailsComponent
   */
  @Input() public alliance: Alliance;
  @ViewChild('confirmDialog') public confirmDialog: WidgetConfirmationDialogComponent;

  public members: User[];
  public currentUser: User;
  public vConfirmDeleteText: string;

  public constructor(
    private _allianceService: AllianceService,
    private _userStorage: UserStorage,
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
   * @returns {Promise<void>}
   * @memberof AllianceDetailsComponent
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
   * @returns {Promise<AllianceJoinRequest>}
   * @memberof AllianceDetailsComponent
   */
  public clickRequestJoin(): Promise<AllianceJoinRequest> {
    return this._allianceService.requestJoin(this.alliance.id).toPromise();
  }

  /**
   * Leaves an alliance
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @memberof AllianceDetailsComponent
   */
  public clickLeave(): void {
    this._loadingService.addPromise(this._allianceService.leave().toPromise());
  }

  /**
   * Executed when the deletation has been confirmed or rejected
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param {boolean} result
   * @memberof AllianceDetailsComponent
   */
  public async onDeleteConfirm(result: boolean): Promise<void> {
    if (result) {
      await this._loadingService.addPromise(this._allianceService.delete(this.alliance.id).toPromise());
      this.confirmDialog.hide();
      this._router.navigate(['/alliance/browse']);
    }
  }
}
