import { Injectable } from '@angular/core';
import { StorageOfflineHelper, AbstractWebsocketApplicationHandler } from '@owge/core';

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

    public constructor() {
        super();
        this._universePrefix = window.document.baseURI + '_';
        this._eventsMap = {
            cache_full_clear_event: '_onCacheFullClearEvent'
        };
    }

    /**
     * Gets the cache store for the given universe
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @template T
     * @param storeName
     * @returns
     */
    public getStore<T>(storeName: string): StorageOfflineHelper<T> {
        return new StorageOfflineHelper(this._universePrefix + storeName, 'local');
    }

    protected _onCacheFullClearEvent(): void {
        Object.keys(localStorage)
            .filter(key => key.indexOf(StorageOfflineHelper.getStartingPrefix() + this._universePrefix) === 0)
            .forEach(key => localStorage.removeItem(key));
    }
}
