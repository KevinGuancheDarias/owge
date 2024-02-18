import { Injectable } from '@angular/core';
import { ReplaySubject } from 'rxjs';
import { SessionStore, OwgeUserModule } from '@owge/core';
import { User, Improvement } from '@owge/types/core';

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
     * Has the user improvements (may update on events, so should keep subscribed)
     *
     * @since 0.8.0
     */
    public readonly currentUserImprovements: ReplaySubject<Improvement> = new ReplaySubject(1);

    /**
     * Current JWT token
     *
     * @since 0.7.0
     */
    public readonly currentToken: ReplaySubject<string> = new ReplaySubject(1);

    public constructor(private _sessionStore: SessionStore) {
        this._sessionStore.addSubject('currentUser', this.currentUser);
        this._sessionStore.addSubject('currentToken', this.currentToken);
        this._sessionStore.addSubject('currentUserImprovements', this.currentUserImprovements);
    }
}
