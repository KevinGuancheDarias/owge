import { Injectable } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import * as io from 'socket.io-client';

import {
  AbstractWebsocketApplicationHandler, AsyncCollectionUtil, LoadingService, LoggerHelper,
  ProgrammingError, SessionService, SessionStore, StorageOfflineHelper
} from '@owge/core';
import { BehaviorSubject, Observable, ReplaySubject, Subject } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { WebsocketSyncResponse } from '../types/websocket-sync-response.type';
import { UniverseCacheManagerService } from './universe-cache-manager.service';
import { WsEventCacheService } from './ws-event-cache.service';
import { UniverseGameService } from './universe-game.service';

@Injectable()
export class WebsocketService {

  private static readonly PROTOCOL_VERSION = '0.1.0';

  /**
   *
   * @readonly
   * @since 0.9.0
   */
  public get isConnected(): Observable<boolean> {
    return this._isConnected.asObservable();
  }

  public get isCachePanic(): Observable<boolean> {
    return this._isCachePanic.asObservable();
  }

  private _socket: SocketIOClient.Socket;
  private _isFirstConnection = true;
  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);
  private _credentialsToken: string;
  private _eventHandlers: Set<AbstractWebsocketApplicationHandler> = new Set;
  private _isAuthenticated = false;
  private _isConnected: Subject<boolean> = new ReplaySubject(1);
  private _hasTriggeredFirtsOffline = false;
  private _isWantedDisconnection: boolean;
  private _isCachePanic: Subject<boolean> = new BehaviorSubject(false);
  private isConnectedInternal = false;
  private isClearingCache = false;

  // Reconnect backoff + resync throttle, so a flapping connection can't hammer the
  // (heavy) websocket-sync endpoint. See _scheduleReconnect and _setupSync.
  private static readonly _RECONNECT_BACKOFF_BASE_MS = 1000;
  private static readonly _RECONNECT_BACKOFF_MAX_MS = 30000;
  private static readonly _MIN_RESYNC_INTERVAL_MS = 10000;
  private _reconnectAttempts = 0;
  private _reconnectTimer: any = null;
  private _lastSyncAt = 0;

  // Buffer for deliver_message events that arrive before the client is ready to
  // process them (i.e. before auth + initial sync have completed).
  private _messageBuffer: any[] = [];
  private _isReadyForMessages = false;

  // Guard against stacking multiple suspension-triggered clearCache waiters.
  private _pendingSuspensionClear = false;

  public constructor(
    private _wsEventCacheService: WsEventCacheService,
    private _sessionService: SessionService,
    private _toastrService: ToastrService,
    private _loadingService: LoadingService,
    private _universeCacheManager: UniverseCacheManagerService,
    private universeGameService: UniverseGameService,
    sessionStore: SessionStore
  ) {
    this._isConnected.next(false);
    this._isConnected.subscribe(sessionStore.isConnected.next.bind(sessionStore.isConnected));
    this._isConnected.subscribe(val => this.isConnectedInternal = val);
    this.setupBackgroundSuspensionDetector();
  }

  public addEventHandler(...handlers: AbstractWebsocketApplicationHandler[]) {
    handlers.forEach(handler => {
      this.onCachePanic(handler);
      this._eventHandlers.add(handler);
    });
  }

  /**
   * Preprends to the beggining
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param handler
   */
  public preprendEventHandler(handler: AbstractWebsocketApplicationHandler): void {
    this.onCachePanic(handler);
    this._eventHandlers = new Set([handler, ...this._eventHandlers]);
  }

  /**
   * Inits the websocket <br>
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param [targetUrl] Only required if connecting for the first time
   * @returns Solves when the socket is properly connected to the backend
   */
  public initSocket(targetUrl?: string, jwtToken?: string): Promise<void> {
    this._isWantedDisconnection = false;
    // Only overwrite the stored token when a token was explicitly provided.
    // This prevents a reconnect (which passes no token) from clobbering a
    // fresher token that was set via setAuthenticationToken() after the
    // original connection was made.
    if (jwtToken !== undefined && jwtToken !== null) {
      this.setAuthenticationToken(jwtToken);
    }
    return new Promise<void>(resolve => {
      if (!this._socket) {
        if (!targetUrl) {
          throw new ProgrammingError('targetUrl MUST be specified at least in first executions');
        }
        this._log.debug('Connecting to remote websocket server', targetUrl);
        this._socket = io.connect({
          path: targetUrl,
          reconnection: true,
          reconnectionDelay: 1000,
          reconnectionDelayMax: 3000,
          reconnectionAttempts: Number.MAX_SAFE_INTEGER
        });

        // Register deliver_message immediately so no messages are lost during
        // the auth + initial-sync window.  Messages received before the client
        // is ready are buffered and flushed once _runWithSyncedData() finishes.
        this._socket.on('deliver_message', message => {
          this._handleDeliverMessage(message);
        });

        this._socket.on('cache_clear', async () => {
          await this.clearCache();
        });

        this._socket.io.on('connect_error', async () => {
          if (this._isFirstConnection && !this._hasTriggeredFirtsOffline) {
            await this._wsEventCacheService.createStores();
            await Promise.all([...this._eventHandlers].map(current => current.createStores()));
            await Promise.all([...this._eventHandlers].map(current => current.workaroundInitialOffline()));
            this._hasTriggeredFirtsOffline = true;
          }
        });
        this._socket.on('connect', async () => {
          if (this._isFirstConnection) {
            this._log.info('Connection established with success');
            this._isFirstConnection = false;
          } else {
            this._log.info('Reconnected');
          }
          this._clearReconnectTimer();

          try {
            await this.authenticate();
          } catch (e) {
            // authenticate() logged the error and cleaned up the socket.
            // Schedule a retry so the client doesn't stay deaf indefinitely.
            // The outer initSocket promise is intentionally NOT resolved here —
            // the caller should await the reconnect path instead.
            this._scheduleReconnect(targetUrl);
            return;
          }
          // Reset the backoff only once authentication succeeded, so repeated
          // connect-then-auth-fail cycles keep backing off instead of looping at
          // the base delay.
          this._reconnectAttempts = 0;
          resolve();
          this._isConnected.next(true);
        });
        this._socket.on('disconnect', () => {
          if (this._isWantedDisconnection) {
            this._log.info('client voluntary disconnected');
          } else {
            this._log.info('client unexpedctly disconnected');
            this._isAuthenticated = false;
            this._isConnected.next(false);
            this._isReadyForMessages = false;
            this._messageBuffer = [];
            this._socket.removeAllListeners();
            this._socket.close();
            delete this._socket;
            this._scheduleReconnect(targetUrl);
          }
        });
      } else if (!this._socket.connected) {
        this._socket.connect();
      } else {
        this._log.debug('It\'s already connected there is no need to reconnect again');
        resolve();
      }
    });
  }

  public setAuthenticationToken(jwtToken: string): void {
    this._credentialsToken = jwtToken;
  }

  /**
   * Schedules a reconnection with exponential backoff (capped), instead of reconnecting
   * immediately. A server that keeps dropping connections would otherwise cause a tight
   * reconnect loop, and every reconnect triggers a websocket-sync; the backoff keeps that
   * under control. Concurrent disconnect events collapse into a single pending attempt.
   * Does NOT pass a token — it uses the current _credentialsToken at reconnect time so
   * a token refresh between disconnect and reconnect is honoured.
   */
  private _scheduleReconnect(targetUrl: string): void {
    if (this._reconnectTimer !== null) {
      return;
    }
    const delay = Math.min(
      WebsocketService._RECONNECT_BACKOFF_BASE_MS * Math.pow(2, this._reconnectAttempts),
      WebsocketService._RECONNECT_BACKOFF_MAX_MS
    );
    this._reconnectAttempts++;
    this._log.info(`Reconnecting in ${delay}ms (attempt ${this._reconnectAttempts})`);
    this._reconnectTimer = window.setTimeout(() => {
      this._reconnectTimer = null;
      this.initSocket(targetUrl);
    }, delay);
  }

  private _clearReconnectTimer(): void {
    if (this._reconnectTimer !== null) {
      window.clearTimeout(this._reconnectTimer);
      this._reconnectTimer = null;
    }
  }

  public async authenticate(): Promise<void> {
    if (!this._isAuthenticated) {
      this._log.debug('starting authentication');
      return await new Promise<void>((resolve, reject) => {
        // Register the response listener BEFORE emitting to avoid a race where
        // the server replies before the listener is attached.
        this._socket.on('authentication', async response => {
          this._socket.removeEventListener('authentication');
          if (response.status === 'ok') {
            this._log.debug('authenticated succeeded');
            await this._universeCacheManager.loadUser();
            const universeIdStore: StorageOfflineHelper<string> = this._universeCacheManager.getStore('ws.universe_id', 'local');
            const universeId = response.value.find(entry => entry.eventName.startsWith('_universe_id:')).eventName.split(':')[1];
            const storedUniverseId: string = await universeIdStore.find();
            if (storedUniverseId && storedUniverseId !== universeId) {
              this._universeCacheManager.clearCachesForUser();
              this._log.warn('Universe changed, even if the origin is the same');
              universeIdStore.save(universeId);
              // TODO: Remove the workaround with window reload, as the Dexie databases don't want to recreate after deletion
              // window.location.reload();
            }
            await this._setupSync(response);
            this._isAuthenticated = true;
            await this._registerSocketHandlers();
            resolve();
          } else if (response.value === 'Invalid credentials') {
            this.close();
            this._sessionService.logout();
          } else {
            this._log.warn('An error occuring while trying to authenticate, response was', response);
            // For non-credential errors (e.g. "invalid token sent from client") we
            // must not leave the socket in a deaf state.  Close and let the caller
            // schedule a reconnect.
            this._isAuthenticated = false;
            this._isConnected.next(false);
            this._isReadyForMessages = false;
            this._messageBuffer = [];
            if (this._socket) {
              this._socket.removeAllListeners();
              this._socket.close();
              delete this._socket;
            }
            reject(response);
          }
        });
        this._socket.emit('authentication', JSON.stringify({
          value: this._credentialsToken,
          protocol: WebsocketService.PROTOCOL_VERSION,
        }));
      });
    }
  }

  /**
   * Closes the socket
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   */
  public close(): void {
    if (this._socket) {
      this._isWantedDisconnection = true;
      this._socket.close();
      this._socket.removeAllListeners();
      this._isFirstConnection = true;
      this._isAuthenticated = false;
      this._isReadyForMessages = false;
      this._messageBuffer = [];
      this._hasTriggeredFirtsOffline = false;
      delete this._socket;
    }
  }

  /**
   * Clear browser cache for current universe and user
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returns
   */
  public async clearCache(): Promise<void> {
    if(!this.isClearingCache) {
      this.isClearingCache = true;
      try {
        this._log.info('Full cache clear, just wanted');
        await this._wsEventCacheService.clearCaches();
        await this._setupSync({value: []}, true);
        await this._runWithSyncedData();
      } finally {
        this.isClearingCache = false;
      }
    } else {
      this._log.warn('already clearing cache');
    }
  }

  private _getValidHandlers(event: keyof WebsocketSyncResponse): AbstractWebsocketApplicationHandler[] {
    return [...this._eventHandlers].filter(
      current => !!current.getHandlerMethod(event)
    );
  }

  /**
   * Handles a deliver_message event.  If the client is not yet ready (auth +
   * initial sync still in progress) the message is queued; once ready it is
   * processed immediately.
   */
  private _handleDeliverMessage(message: any): void {
    if (!this._isReadyForMessages) {
      this._messageBuffer.push(message);
      return;
    }
    this._processDeliverMessage(message);
  }

  private _processDeliverMessage(message: any): void {
    this._log.debug('An event from backend server received', message);
    if (message && message.status && message.eventName) {
      const eventName = message.eventName;
      const handlers: AbstractWebsocketApplicationHandler[] = this._getValidHandlers(eventName);
      if (handlers.length) {
        // Save once per message, outside the handler loop, so we don't write
        // the same data N times when multiple handlers subscribe to the event.
        this._wsEventCacheService.saveEventData(message, message.value).then(() => {
          handlers.forEach(async handler => {
            try {
              await handler.execute(eventName, message.value);
            } catch (e) {
              this._log.error(`Handler ${handler.constructor.name} failed for event ${eventName}`, e);
            }
          });
        }).catch(e => {
          this._log.error(`saveEventData failed for event ${eventName}`, e);
        });
      } else {
        this._log.error('No handler for event ' + eventName, message);
      }
    } else {
      this._log.warn('Bad message from backend', message);
    }
  }

  private async _registerSocketHandlers(): Promise<void> {
    try {
      await Promise.all([
        ...[...this._eventHandlers].map(handler => handler.beforeWorkaroundSync())
      ]);
      await Promise.all([...this._eventHandlers].map(handler => handler.createStores()));
      // Await the initial sync so buffered live messages are processed AFTER
      // the initial data is in place, not racing against it.
      await this._runWithSyncedData();
    } catch (e) {
      this._log.error('Workaround WS sync failed ', e);
    }

    // Mark the client as ready and flush any messages buffered during auth + sync.
    this._isReadyForMessages = true;
    const buffered = this._messageBuffer.splice(0);
    this._log.debug(`Flushing ${buffered.length} buffered deliver_message(s)`);
    buffered.forEach(msg => this._processDeliverMessage(msg));
  }

  private _timeoutPromise(inputPromise: Promise<any>): Promise<any> {
    return Promise.race([
      inputPromise,
      new Promise(resolve => window.setTimeout(() => resolve('timeout'), 20000))
    ]);
  }

  private async _setupSync(response: { value: any[] }, force = false): Promise<void> {
    try {
      await this._loadingService.runWithLoading(async () => {
        await this._wsEventCacheService.createStores();
        await this._wsEventCacheService.setEventsInformation(response.value);
        await this._wsEventCacheService.createOfflineStores();
        const now = Date.now();
        if (force || now - this._lastSyncAt >= WebsocketService._MIN_RESYNC_INTERVAL_MS) {
          this._lastSyncAt = now;
          await this._wsEventCacheService.applySync();
        } else {
          this._log.debug('Skipping resync, throttled to avoid hammering websocket-sync');
        }
        this._isCachePanic.next(false);
      });
    } catch (e) {
      this._log.error('Cache panic on sync', e);
      this._toastrService.error(`Cache panic ${e.message}`);
      this._isCachePanic.next(true);
    }
  }

  /**
   *
   * @todo In the future run only with changed events
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @private
   * @returns
   */
  private async _runWithSyncedData(): Promise<void> {
    await this.syncOpenEvents();
    await AsyncCollectionUtil.forEach([...this._eventHandlers], async handler => {
      const eventMap = handler.getEventsMap();
      const failedHandlers: string[] = [];
      await AsyncCollectionUtil.forEach(Object.keys(eventMap), async (event: keyof WebsocketSyncResponse) => {
        // Initial run with synced data
        if (this._wsEventCacheService.isSynchronizableEvent(event)
          || WsEventCacheService.eventsForOpen.includes(event)
        ) {
          this._log.debug(`Running handler for event ${event}, known to be in ${handler.constructor.name}`);
          try {
            let storedValue = await this._wsEventCacheService.findStoredValue(event);
            if ((storedValue === null || storedValue === undefined)
              && this._wsEventCacheService.isSynchronizableEvent(event)
            ) {
              // The event information cache claims this event is up to date, but its offline data
              // store is empty (a desync, typically after a backend restart). Re-fetch the data
              // from the backend before running the handler instead of feeding it empty content.
              this._log.warn(`Desync detected for event ${event}: cached data missing, refetching from backend`);
              storedValue = await this._wsEventCacheService.refetchEvent(event);
            }
            if ((storedValue === null || storedValue === undefined)
              && this._wsEventCacheService.isSynchronizableEvent(event)
            ) {
              // Still no data after the refetch: drop the stale event information so it is retried
              // on the next sync, and skip the handler to avoid poisoning its store with empty data.
              this._log.error(`No data available for synchronizable event ${event} after refetch, skipping handler`);
              failedHandlers.push(event);
              return;
            }
            const result = await this._timeoutPromise(
              handler.execute(event, storedValue)
            );
            if (result === 'timeout') {
              this._toastrService.warning(`Sync took too much for ${handler.constructor.name} on event ${event}`);
            }
          } catch (e) {
            this._toastrService.error(`Sync failed for ${handler.constructor.name} on event ${event}`);
            this._log.error(e);
            failedHandlers.push(event);
          }
        }
      });
      await this._wsEventCacheService.deleteEvents(...failedHandlers);
    });
  }

  private async syncOpenEvents(): Promise<void> {
    await AsyncCollectionUtil.forEach(WsEventCacheService.eventsForOpen, async event => {
        const content = await this.universeGameService.getToUniverse(`open/websocket-sync/${event}`).toPromise();
        await this._wsEventCacheService.updateOfflineStore(event, content);
    });
  }

  private onCachePanic(handler: AbstractWebsocketApplicationHandler): void {
    handler.onCachePanic(() => {
      this._isCachePanic.next(true);
      this._log.error(`Handler marked the connection as panic ${handler.constructor.name}`);
    });
  }

  private setupBackgroundSuspensionDetector(): void {
    let currentDate = new Date().getTime();
    setInterval(async () => {
      const now = new Date().getTime();
      const diff = (now - currentDate) / 1000;
      currentDate = now;
      if(diff > 3) {
        await this.maybeTriggerCache();
      }
    },1000);
  }

  private async maybeTriggerCache(): Promise<void> {
    // Guard: only one pending suspension-triggered clear at a time.
    if (this._pendingSuspensionClear) {
      return;
    }
    this._pendingSuspensionClear = true;
    try {
      if (this.isConnectedInternal) {
        // Already connected — clear once immediately.
        await this.clearCache();
        this._log.info('Clear cache due to background suspension (was connected)');
      } else {
        // Not connected yet — wait for the next connected=true emission, then
        // clear once.  Use a one-shot subscription so we don't stack listeners.
        await new Promise<void>(resolve => {
          this._isConnected.pipe(
            filter(v => v),
            take(1)
          ).subscribe(async () => {
            await this.clearCache();
            this._log.info('Clear cache due to background suspension (waited for reconnect)');
            resolve();
          });
        });
      }
    } finally {
      this._pendingSuspensionClear = false;
    }
  }
}
