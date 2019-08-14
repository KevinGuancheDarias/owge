import { Injectable } from '@angular/core';
import { ReplaySubject } from 'rxjs';

import { User } from '../types/user.type';

/**
 * Stores logged in user information
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @class UserStorage
 */
@Injectable()
export class UserStorage {

    /**
     * Current logged in user
     *
     * @type {ReplaySubject<User>}
     * @since 0.7.0
     * @memberof UserStorage
     */
    public readonly currentUser: ReplaySubject<User> = new ReplaySubject(1);


    /**
     * Current JWT token
     *
     * @type {ReplaySubject<string>}
     * @since 0.7.0
     * @memberof UserStorage
     */
    public readonly currentToken: ReplaySubject<string> = new ReplaySubject(1);
}
