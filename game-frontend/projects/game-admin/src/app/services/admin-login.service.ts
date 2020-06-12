import { Injectable } from '@angular/core';
import { Observable, ReplaySubject } from 'rxjs';

import { LoginService, JwtTokenUtil } from '@owge/core';
import { UniverseGameService } from '@owge/universe';

import { AdminUserStore } from '../store/admin-user.store';
import { take } from 'rxjs/operators';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable()
export class AdminLoginService {
    public static readonly SESSION_STORAGE_KEY = 'owge_admin_authentication';
    public constructor(
        private _loginService: LoginService,
        private _adminUserStore: AdminUserStore,
        private _universeGameService: UniverseGameService
    ) { }

    /**
     * Logins to the admin panel
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param mail Account system email
     * @param password Account system password
     * @returns The admin JWT token (Not to confuse with user JWT token)
     */
    public login(mail: string, password: string): Observable<String> {
        const subject: ReplaySubject<String> = new ReplaySubject(1);
        this._loginService.login(mail, password).subscribe(() => {
            this._universeGameService.requestWithAutorizationToContext('game', 'post', 'adminLogin', 'ignored').subscribe(({ token }) => {
                this.defineToken(token);
                subject.next(token);
            });
        });
        return subject.asObservable().pipe(take(1));
    }

    /**
     * Defines the admin user token (updates the AdminUserStore)
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param rawToken
     */
    public defineToken(rawToken: string): void {
        this._adminUserStore.adminUser.next(<any>JwtTokenUtil.parseToken(rawToken).data);
        this._adminUserStore.adminToken.next(rawToken);
        sessionStorage.setItem(AdminLoginService.SESSION_STORAGE_KEY, rawToken);
    }
}
