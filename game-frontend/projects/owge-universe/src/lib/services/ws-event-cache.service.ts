import { Injectable } from '@angular/core';
import { StorageOfflineHelper } from '@owge/core';
import { UniverseCacheManagerService } from './universe-cache-manager.service';

interface WebsocketEventInformation {
    eventName: string;
    userId: number;
    lastSent: number;

    /**
     * Changed computes at browser by comparing cached lastSent with remote
     *
     * @since 0.9.0
     */
    changed: boolean;
}

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Injectable()
export class WsEventCacheService {
    private static readonly _NULL_LIKE_VALUE = '\u19fa__boundary__\u14af__\u1111';
    private _eventsInformation: { [key: string]: WebsocketEventInformation };
    private _offlineStore: StorageOfflineHelper<{ [key: string]: WebsocketEventInformation }>;

    public constructor(universeCacheManagerService: UniverseCacheManagerService) {
        this._offlineStore = universeCacheManagerService.getStore('ws_event_cache.entries');
        this._eventsInformation = {};
        this._offlineStore.doIfNotNull(data => this._eventsInformation = data);
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param content
     */
    public setEventsInformation(content: WebsocketEventInformation[]): void {
        Object.keys(this._eventsInformation).forEach(key => {
            if (!content.some(current => current.eventName === key)) {
                delete this._eventsInformation[key];
            }
        });
        content.forEach(current => {
            current.changed = !this._eventsInformation[current.eventName]
                || this._eventsInformation[current.eventName].lastSent !== current.lastSent;
            this._eventsInformation[current.eventName] = current;
        });
        this._offlineStore.save(this._eventsInformation);
    }


    /**
     * Returns true if the entry has changed, known because it exists, and has the change prop defined
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param eventName
     * @returns
     */
    public hasChanged(eventName: string): boolean {
        const entry = this._eventsInformation[eventName];
        return entry && entry.changed;
    }

    /**
     * Finds value or runs action if not in cache
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @template T
     * @param eventName The websocket event name
     * @param cacheStore The store that holds or saves the value sent by the event
     * @param action Action to do to refresh the value (tipically an HTTPClient get request)
     * @returns
     */
    public async findFromCacheOrRun<T>(eventName: string, cacheStore: StorageOfflineHelper<T>, action: () => Promise<T>): Promise<T> {
        const entry = this._eventsInformation[eventName];
        const storedValue = cacheStore.find();
        if ((!entry || !entry.changed) && storedValue) {
            return <any>storedValue === WsEventCacheService._NULL_LIKE_VALUE
                ? null
                : storedValue;
        } else {
            const retVal: T = await action();
            cacheStore.save(retVal || <any>WsEventCacheService._NULL_LIKE_VALUE);
            if (this._eventsInformation[eventName]) {
                this._eventsInformation[eventName].changed = false;
            } else {
                this._eventsInformation[eventName] = {
                    changed: false,
                    eventName,
                    lastSent: -1,
                    userId: -1
                };
            }
            this._offlineStore.save(this._eventsInformation);
            return retVal;
        }
    }
}
