import { HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AsyncCollectionUtil, ProgrammingError, SessionService, StorageOfflineHelper } from '@owge/core';
import { WebsocketSyncResponse } from '../types/websocket-sync-response.type';
import { UniverseCacheManagerService } from './universe-cache-manager.service';
import { UniverseGameService } from './universe-game.service';

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
    private static readonly _ALLOWED_EVENTS: Array<keyof WebsocketSyncResponse> = [
        'planet_owned_change',
        'running_upgrade_change',
        'enemy_mission_change',
        'upgrade_types_change',
        'planet_user_list_change',
        'tutorial_entries_change',
        'time_special_change',
        'unit_unlocked_change',
        'unit_type_change',
        'mission_report_change',
        'missions_count_change',
        'twitch_state_change',
        'unit_mission_change',
        'speed_impact_group_unlocked_change',
        'unit_build_mission_change',
        'obtained_upgrades_change',
        'unit_obtained_change',
        'unit_requirements_change',
        'visited_tutorial_entry_change',
        'user_data_change',
    ];
    private _eventsInformation: { [key: string]: WebsocketEventInformation };
    private _eventInformationStore: StorageOfflineHelper<{ [key: string]: WebsocketEventInformation }>;
    private _eventsOfflineStore: { [key: string]: StorageOfflineHelper<any> } = {};

    public constructor(
        private _universeCacheManagerService: UniverseCacheManagerService,
        private _universeGameService: UniverseGameService,
        _sessionService: SessionService
    ) {
        this._eventsInformation = {};
        _sessionService.onLogout.subscribe(() => this._clearSession());
    }

    /**
     * Creates the store, make sure to use it in WebsocketService before settings eventsInformation
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public async createStores(): Promise<void> {
        if (!this._eventInformationStore) {
            this._eventInformationStore = this._universeCacheManagerService.getStore('ws_event_cache.entries');
            await this._eventInformationStore.doIfNotNull(data => this._eventsInformation = data);
        }
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param content
     */
    public async setEventsInformation(content: WebsocketEventInformation[]): Promise<void> {
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
        this._eventInformationStore.save(this._eventsInformation);
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
    public async findFromCacheOrRun<T>(
        eventName: keyof WebsocketSyncResponse,
        cacheStore: StorageOfflineHelper<T>,
        action: () => Promise<T>
    ): Promise<T> {
        const entry = this._eventsInformation[eventName];
        const storedValue = await cacheStore.find();
        if (await this._isValidCacheEntry(entry, cacheStore)) {
            return storedValue;
        } else {
            const retVal: T = await action();
            await cacheStore.save(retVal);
            this._markEventAsUnchanged(eventName);
            await this._eventInformationStore.save(this._eventsInformation);
            return retVal;
        }
    }

    public async createOfflineStores(): Promise<void> {
        await this._universeCacheManagerService.beforeWorkaroundSync();
        WsEventCacheService._ALLOWED_EVENTS.forEach(event => {
            if (!this._eventsOfflineStore[event]) {
                this._eventsOfflineStore[event] = this._universeCacheManagerService.getStore(this._findCacheKey(event));
            }
        });
    }

    public applySync(): Promise<void> {
        return new Promise(async resolve => {
            if (this._eventsInformation) {
                const wantedKeys: Array<keyof WebsocketSyncResponse> = await AsyncCollectionUtil
                    .filter(WsEventCacheService._ALLOWED_EVENTS, async event =>
                        this.isSynchronizableEvent(event)
                        && !(await this._isValidCacheEntry(this._eventsInformation[event], this._eventsOfflineStore[event]))
                    );
                this._universeGameService.requestWithAutorizationToContext<WebsocketSyncResponse>(
                    'game',
                    'get',
                    'websocket-sync',
                    null,
                    {
                        params: new HttpParams().append('keys', wantedKeys.join(','))
                    }
                ).subscribe(async events => {
                    const keys: Array<keyof WebsocketSyncResponse> = <any>Object.keys(events);
                    await AsyncCollectionUtil.forEach(keys, async key => {
                        await this._eventsOfflineStore[key].save(events[key]);
                        this._markEventAsUnchanged(key);
                    });
                    await this._eventInformationStore.save(this._eventsInformation);
                    resolve();
                });
            } else {
                throw new ProgrammingError('Should never invoke this method before loading the event information');
            }
        });
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.6
     * @template T
     * @param event
     * @returns
     */
    public async findStoredValue<T = any>(event: keyof WebsocketSyncResponse): Promise<T> {
        return await this._eventsOfflineStore[event].find();
    }

    /**
     * Updates the information without making the store as updated <br>
     * <b>This can be used to add frontend-computed properties such as termination date</b>
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.6
     * @param event
     * @param data
     * @returns
     */
    public async updateWithFrontendComputedData(event: keyof WebsocketSyncResponse, data: any): Promise<void> {
        await this._eventsOfflineStore[event].save(data);
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.6
     * @param {keyof WebsocketSyncResponse} event
     * @param data
     * @returns
     */
    public async updateOfflineStore(event: keyof WebsocketSyncResponse, data: any): Promise<void> {
        await this._eventsOfflineStore[event].save(data);
        this._markEventAsUnchanged(event);
        await this._eventInformationStore.save(this._eventsInformation);
    }

    /**
     * When a socket event is received, should update the stores
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.6
     * @param {WebsocketEventInformation} eventData
     * @param {*} data
     * @returns {Promise<void>}
     */
    public async saveEventData(eventData: WebsocketEventInformation, data: any): Promise<void> {
        if (this.isSynchronizableEvent(eventData.eventName)) {
            await this._eventsOfflineStore[eventData.eventName].save(data);
        }
        this._eventsInformation[eventData.eventName] = {
            eventName: eventData.eventName,
            changed: false,
            lastSent: eventData.lastSent,
            userId: -1
        };
        await this._eventInformationStore.save(this._eventsInformation);
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.6
     * @returns
     */
    public async clearCaches(): Promise<void> {
        this._eventsInformation = {};
        await AsyncCollectionUtil.forEach(Object.keys(this._eventsOfflineStore), async event => {
            await this._eventsOfflineStore[event].delete();
        });
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.6
     * @param  event
     * @returns
     */
    public isSynchronizableEvent(event: string): boolean {
        return WsEventCacheService._ALLOWED_EVENTS.includes(<any>event);
    }

    private async _isValidCacheEntry(entry: WebsocketEventInformation, offlineStore: StorageOfflineHelper<any>): Promise<boolean> {
        return (!entry || !entry.changed) && await offlineStore.isPresent();
    }

    private _findCacheKey(event: keyof WebsocketSyncResponse): string {
        return `ws_cache::${event}`;
    }

    private _markEventAsUnchanged(event: keyof WebsocketSyncResponse): void {
        if (this._eventsInformation[event]) {
            this._eventsInformation[event].changed = false;
        } else {
            this._eventsInformation[event] = {
                changed: false,
                eventName: event,
                lastSent: -1,
                userId: -1
            };
        }
    }

    private _clearSession(): void {
        this._eventsInformation = {};
        this._eventInformationStore = null;
        this._eventsOfflineStore = {};
    }
}
