import { Injectable } from '@angular/core';
import { ReplaySubject, Subject } from 'rxjs';

import { SessionStore } from '@owge/core';
import { AdminUser } from '../types/admin-user.type';

/**
 * Stores admin user related info
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable({ providedIn: 'root' })
export class AdminUserStore {

    /**
     * Currently logged admin user
     *
     * @since 0.8.0
     */
    public readonly adminUser: ReplaySubject<AdminUser> = new ReplaySubject(1);

    /**
     * Currently logged admin token
     *
     * @since 0.8.0
     */
    public readonly adminToken: ReplaySubject<string> = new ReplaySubject(1);

    /**
     * Represents the admin users of this universe
     *
     * @since 0.9.0
     */
    public readonly added: Subject<AdminUser[]> = new ReplaySubject(1);

    /**
     * Represents the users that can be added as admins
     *
     * @since 0.9.0
     */
    public readonly available: Subject<AdminUser[]> = new ReplaySubject(1);

    public constructor(private _sessionStore: SessionStore) {
        this._sessionStore.addSubject('adminUser', this.adminUser);
        this._sessionStore.addSubject('adminToken', this.adminToken);
    }
}
