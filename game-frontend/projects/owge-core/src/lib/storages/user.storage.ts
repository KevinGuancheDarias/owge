import { Injectable } from '@angular/core';
import { ReplaySubject } from 'rxjs';

import { User } from '../types/user.type';
import { OwgeUserModule } from '../owge-user.module';
import { SessionStore } from '../store/session.store';

/**
 * Stores logged in user information
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
@Injectable({
    providedIn: OwgeUserModule
})
export class UserStorage<U extends User> {

    /**
     * Current logged in user
     *
     * @since 0.7.0
     */
    public readonly currentUser: ReplaySubject<U> = new ReplaySubject(1);

    /**
     * Current JWT token
     *
     * @since 0.7.0
     */
    public readonly currentToken: ReplaySubject<string> = new ReplaySubject(1);

    public constructor(private _sessionStore: SessionStore) {
        this._sessionStore.addSubject('currentUser', this.currentUser);
        this._sessionStore.addSubject('currentToken', this.currentToken);
    }
}
