import { HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AsyncCollectionUtil, Instant, ProgrammingError, SessionService, StorageOfflineHelper } from '@owge/core';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CacheListener } from '../interfaces/cache-listener.interface';
import { WebsocketSyncItem, WebsocketSyncResponse } from '../types/websocket-sync-response.type';
import { UniverseCacheManagerService } from './universe-cache-manager.service';
import { UniverseGameService } from './universe-game.service';

interface WebsocketEventInformation {
    eventName: string;
    userId: number;
    lastSent: Instant;

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
        'system_message_change'
    ];
    private _eventsInformation: { [key: string]: WebsocketEventInformation };
    private _eventInformationStore: StorageOfflineHelper<{ [key: string]: WebsocketEventInformation }>;
    private _eventsOfflineStore: { [key: string]: StorageOfflineHelper<any> } = {};
    private _cacheListeners: CacheListener[] = [];

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
            if(current.eventName === 'unit_obtained_change') {
                console.log('Usando valor almacenado', this._eventsInformation[current.eventName]?.lastSent?.epochSecond);
                console.log('Valor que comparo con respuesta', current?.lastSent?.epochSecond);
            }
            if(!current.eventName.startsWith('_universe_id:')) {
                current.changed = !this._eventsInformation[current.eventName]
                    || this._eventsInformation[current.eventName].lastSent.epochSecond !== current.lastSent.epochSecond;
            }
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

    public async createOfflineStores(): Promise<void> {
        await this._universeCacheManagerService.beforeWorkaroundSync();
        WsEventCacheService._ALLOWED_EVENTS.forEach(event => {
            if (!this._eventsOfflineStore[event]) {
                this._eventsOfflineStore[event] = this._universeCacheManagerService.getStore(this._findCacheKey(event));
            }
        });
    }

    public applySync(): Promise<void> {
        return new Promise(async (resolve, reject) => {
            if (this._eventsInformation) {
                const wantedKeys: Array<keyof WebsocketSyncResponse> = await AsyncCollectionUtil
                    .filter(WsEventCacheService._ALLOWED_EVENTS, async event =>
                        this.isSynchronizableEvent(event)
                            && (!this._eventsInformation[event] || this._eventsInformation[event].changed)
                    );
                if (wantedKeys.length) {
                    this._universeGameService.requestWithAutorizationToContext<WebsocketSyncResponse>(
                        'game',
                        'get',
                        'websocket-sync',
                        null,
                        {
                            params: new HttpParams().append('keys', wantedKeys.join(','))
                        }
                    ).pipe(
                        catchError(err => {
                            reject(err);
                            return throwError(err);
                        })
                    ).subscribe(async events => {
                        wantedKeys.filter(key => typeof events[key] === 'undefined').forEach(key => events[key] = null);
                        const keys: Array<keyof WebsocketSyncResponse> = <any>Object.keys(events);
                        await AsyncCollectionUtil.forEach(keys, async key => {
                            await this._eventsOfflineStore[key].save(events[key].data);
                            if(!this._eventsInformation[key]) {
                                this._eventsInformation[key] = {
                                    eventName: key,
                                    changed: false,
                                    userId: -1,
                                    lastSent: { epochSecond: events[key].lastSent}
                                }
                            } else {
                                this._eventsInformation[key].lastSent = { epochSecond: events[key].lastSent };
                            }
                            if(key === 'unit_obtained_change') {
                                console.log('Guardando valor', events[key]?.lastSent);
                            }
                            this._markEventAsUnchanged(key, events[key]);
                        });
                        await this._eventInformationStore.save(this._eventsInformation);
                        resolve();
                    });
                } else {
                    resolve();
                }
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
        await AsyncCollectionUtil.forEach(this._cacheListeners, listener => listener.afterCacheClear());
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

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.15
     * @param listeners
     */
    public addCacheListeners(...listeners: CacheListener[]): void {
        this._cacheListeners = this._cacheListeners.concat(listeners);
    }

    private _findCacheKey(event: keyof WebsocketSyncResponse): string {
        return `ws_cache::${event}`;
    }

    private _markEventAsUnchanged(event: keyof WebsocketSyncResponse, info: WebsocketSyncItem): void {
        if (this._eventsInformation[event]) {
            this._eventsInformation[event].changed = false;
            this._eventsInformation[event].lastSent = { epochSecond: info.lastSent };
        } else {
            this._eventsInformation[event] = {
                changed: false,
                eventName: event,
                lastSent: { epochSecond: info.lastSent },
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
