import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { AdminUserService } from '../../services/admin-user.service';
import { AdminUser } from '../../types/admin-user.type';
import { filter } from 'rxjs/operators';
import { combineLatest } from 'rxjs';
import { ObservableSubscriptionsHelper, ModalComponent, LoadingService } from '@owge/core';
import { AdminUserStore } from '../../store/admin-user.store';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-admin-users',
  templateUrl: './admin-users.component.html',
  styleUrls: ['./admin-users.component.scss']
})
export class AdminUsersComponent implements OnInit, OnDestroy {

  @ViewChild('modal')
  public modal: ModalComponent;

  public selectedAdmin: AdminUser = null;
  public added: AdminUser[];
  public available: AdminUser[];

  private _subscriptions: ObservableSubscriptionsHelper = new ObservableSubscriptionsHelper;

  constructor(
    private _adminUserService: AdminUserService,
    private _loadingService: LoadingService,
    private _adminUserStore: AdminUserStore
  ) { }

  ngOnInit(): void {
    this._subscriptions.add(
      combineLatest(
        this._adminUserStore.adminUser,
        this._adminUserService.findAddedAdmins(),
        (logged, added) => added.filter(current => current.id !== logged.id)
      ).subscribe(users => this.added = users),
      combineLatest(
        this._adminUserService.findAddedAdmins(),
        this._adminUserService.findAccountUsers(),
        (added, available) => available.filter(current => !added.some(addedOne => addedOne.id === current.id))
      ).subscribe(available => this.available = available)
    );
  }

  public ngOnDestroy(): void {
    this._subscriptions.unsubscribeAll();
  }

  public clickAdd(): void {
    this._loadingService.runWithLoading(async () => {
      await this._adminUserService.addAdmin(this.selectedAdmin);
      this.modal.hide();
    });
  }

  public clickDelete(adminUser: AdminUser): void {
    this._loadingService.addPromise(this._adminUserService.removeAdmin(adminUser.id));
  }
}
