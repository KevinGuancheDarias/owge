import { Injectable } from '@angular/core';
import { ReplaySubject } from 'rxjs';

import { User, SessionStore } from '@owge/core';

/**
 * Stores admin user related info
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable({ providedIn: 'root'})
export class AdminUserStore {

    /**
     * Currently logged admin user
     *
     * @since 0.8.0
     */
    public readonly adminUser: ReplaySubject<User> = new ReplaySubject(1);

    /**
     * Currently logged admin token
     *
     * @since 0.8.0
     */
    public readonly adminToken: ReplaySubject<string> = new ReplaySubject(1);

    public constructor(private _sessionStore: SessionStore) {
        this._sessionStore.addSubject('adminUser', this.adminUser);
        this._sessionStore.addSubject('adminToken', this.adminToken);
    }
}
