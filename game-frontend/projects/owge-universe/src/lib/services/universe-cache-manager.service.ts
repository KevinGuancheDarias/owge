import { Injectable } from '@angular/core';
import { StorageOfflineHelper, AbstractWebsocketApplicationHandler, UserStorage, User, JwtTokenUtil } from '@owge/core';
import { filter, map, take } from 'rxjs/operators';
import { WebsocketService } from './websocket.service';
import { UserWithFaction } from '@owge/faction';

/**
 * Holds cached resources for the current universe
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Injectable()
export class UniverseCacheManagerService extends AbstractWebsocketApplicationHandler {
    private _universePrefix;
    private _stores: StorageOfflineHelper<any>[] = [];
    private _userId: number;

    public constructor(private _userStore: UserStorage<UserWithFaction>) {
        super();
        this._universePrefix = window.document.baseURI + '_';
        this._eventsMap = {
            cache_full_clear_event: '_onCacheFullClearEvent'
        };
        this._userId = +sessionStorage.getItem('cache_manager.user');
    }

    public async loadUser(): Promise<void> {
        const user: UserWithFaction = await Promise.race([
            this._userStore.currentToken.pipe(filter(val => !!val), map(val => JwtTokenUtil.parseToken(val).data), take(1)).toPromise(),
            new Promise<any>(resolve => window.setTimeout(resolve, 3000))
        ]);
        if (user) {
            this._userId = user.id;
            sessionStorage.setItem('cache_manager.user', user.id.toString());
        }
    }

    public async beforeWorkaroundSync(): Promise<void> {
        await this.loadUser();
    }

    public async clearCache(): Promise<void> {
        await Promise.all(this._stores.map(current => current.delete()));
    }

    /**
     * Notice. if invoked before or during the workaroundSync unexpected behavior may occur. It's best recommended to use in constructors
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @template T
     * @param storeName
     * @see AbstractWebsocketApplicationHandler::createStores()
     * @returns
     */
    public getStore<T>(storeName: string): StorageOfflineHelper<T> {
        if (!this._userId) {
            this._log.warn('Getting user shared cache, maybe invoking before workaroundSync');
        }
        const retVal: StorageOfflineHelper<T> = new StorageOfflineHelper(
            this._universePrefix + this._userId + '_' + storeName,
            'indexeddb'
        );
        this._stores.push(retVal);
        return retVal;
    }

    protected _onCacheFullClearEvent(): void {
        Object.keys(localStorage)
            .filter(key => key.indexOf(StorageOfflineHelper.getStartingPrefix() + this._universePrefix) === 0)
            .forEach(key => localStorage.removeItem(key));
    }
}
