import { Injectable } from '@angular/core';
import { UniverseGameService } from '@owge/universe';
import { Observable } from 'rxjs';
import { AdminUser } from '../types/admin-user.type';
import { CoreHttpService, OwgeCoreConfig } from '@owge/core';
import { AdminUserStore } from '../store/admin-user.store';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Injectable()
export class AdminUserService {

    private _isInitialAddedCall = true;
    private _isInitialAvailableCall = true;
    private _adminUsers: AdminUser[];

    public constructor(
        private _universeGameService: UniverseGameService,
        private _coreHttpService: CoreHttpService,
        private _accountConfig: OwgeCoreConfig,
        private _adminUserStore: AdminUserStore
    ) {
        _adminUserStore.added.subscribe(added => this._adminUsers = added);
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public findAddedAdmins(): Observable<AdminUser[]> {
        if (this._isInitialAddedCall) {
            this._universeGameService.requestWithAutorizationToContext('admin', 'get', 'admin-user')
                .subscribe(added => this._adminUserStore.added.next(added));
            this._isInitialAddedCall = false;
        }
        return this._adminUserStore.added.asObservable();
    }

    /**
     * Returns the available users from the admin system
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public findAccountUsers(): Observable<AdminUser[]> {
        if (this._isInitialAvailableCall) {
            this._coreHttpService.get(`${this._accountConfig.url}/user/public`)
                .subscribe(available => this._adminUserStore.available.next(available));
            this._isInitialAvailableCall = false;
        }
        return this._adminUserStore.available.asObservable();
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param adminUser
     */
    public async addAdmin(adminUser: AdminUser): Promise<void> {
        const result = await this._universeGameService
            .requestWithAutorizationToContext('admin', 'put', `admin-user/${adminUser.id}`, adminUser).toPromise();
        this._adminUsers.push(result);
        this._adminUserStore.added.next(this._adminUsers);
    }


    /**
     * Removes an admin
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param id
     * @returns
     */
    public async removeAdmin(id: number): Promise<void> {
        await this._universeGameService
            .requestWithAutorizationToContext('admin', 'delete', `admin-user/${id}`).toPromise();
        this._adminUserStore.added.next(this._adminUsers.filter(user => user.id !== id));
    }
}
