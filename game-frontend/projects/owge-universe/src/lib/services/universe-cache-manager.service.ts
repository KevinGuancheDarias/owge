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
        this._userId = +localStorage.getItem(this._universePrefix + 'cache_manager.user');
    }

    public async beforeWorkaroundSync(): Promise<void> {
        const user: UserWithFaction = await Promise.race([
            this._userStore.currentToken.pipe(filter(val => !!val), map(val => JwtTokenUtil.parseToken(val).data), take(1)).toPromise(),
            new Promise<any>(resolve => window.setTimeout(resolve, 1000))
        ]);
        if (user) {
            if (user.id !== this._userId) {
                this._stores.filter(current => current.isUserDependant).forEach(current => current.delete());
                this._userId = user.id;
                localStorage.setItem(this._universePrefix + 'cache_manager.user', user.id.toString());
            }
        }
    }

    public async clearCache(): Promise<void> {
        this._stores.forEach(current => current.delete());
    }

    /**
     * Notice. if invoked after or during the workaroundSync unexpected behavior may occur. It's best recommended to use in constructors
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @template T
     * @param storeName
     * @param [isUserDependant=true] If the cache should be clear in case of other user login
     * @returns
     */
    public getStore<T>(storeName: string, isUserDependant = true): StorageOfflineHelper<T> {
        const retVal: StorageOfflineHelper<T> = new StorageOfflineHelper(this._universePrefix + storeName, 'local');
        retVal.isUserDependant = isUserDependant;
        this._stores.push(retVal);
        return retVal;
    }

    protected _onCacheFullClearEvent(): void {
        Object.keys(localStorage)
            .filter(key => key.indexOf(StorageOfflineHelper.getStartingPrefix() + this._universePrefix) === 0)
            .forEach(key => localStorage.removeItem(key));
    }
}
