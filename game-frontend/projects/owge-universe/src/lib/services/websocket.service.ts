import { Injectable } from '@angular/core';
import * as io from 'socket.io-client';
import { ToastrService } from 'ngx-toastr';

import { Observable, Subject, ReplaySubject, BehaviorSubject } from 'rxjs';
import { WsEventCacheService } from './ws-event-cache.service';
import {
  LoggerHelper, AbstractWebsocketApplicationHandler, ProgrammingError, SessionStore,
  SessionService, LoadingService, StorageOfflineHelper, AsyncCollectionUtil
} from '@owge/core';
import { UniverseCacheManagerService } from './universe-cache-manager.service';
import { WebsocketSyncResponse } from '../types/websocket-sync-response.type';

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

  private _onBeforeWorkaroundSyncHandlers: Array<() => Promise<void>> = [];

  public constructor(
    private _wsEventCacheService: WsEventCacheService,
    private _sessionService: SessionService,
    private _toastrService: ToastrService,
    private _loadingService: LoadingService,
    private _universeCacheManager: UniverseCacheManagerService,
    sessionStore: SessionStore
  ) {
    this._isConnected.next(false);
    this._isConnected.subscribe(sessionStore.isConnected.next.bind(sessionStore.isConnected));
  }

  public addEventHandler(...handlers: AbstractWebsocketApplicationHandler[]) {
    handlers.forEach(handler => {
      handler.onCachePanic(() => this._isCachePanic.next(true));
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
    handler.onCachePanic(() => this._isCachePanic.next(true));
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
    this.setAuthenticationToken(jwtToken);
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

          await this.authenticate();
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
            this._socket.removeAllListeners();
            this._socket.close();
            delete this._socket;
            this.initSocket(targetUrl, jwtToken);
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

  public async authenticate(): Promise<void> {
    if (!this._isAuthenticated) {
      this._log.debug('starting authentication');
      return await new Promise<void>((resolve, reject) => {
        this._socket.emit('authentication', JSON.stringify({
          value: this._credentialsToken,
          protocol: WebsocketService.PROTOCOL_VERSION,
        }));
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
              window.location.reload();
            }
            await this._setupSync(response);
            this._isAuthenticated = true;
            this._registerSocketHandlers();
            resolve();
          } else if (response.value === 'Invalid credentials') {
            this.close();
            this._sessionService.logout();
          } else {
            this._log.warn('An error occuring while trying to authenticate, response was', response);
            reject(response);
          }
        });
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
    this._log.info('Full cache clear, just wanted');
    await this._wsEventCacheService.clearCaches();
    await this._setupSync({ value: [] });
    await this._runWithSyncedData();
  }

  private _getValidHandlers(event: keyof WebsocketSyncResponse): AbstractWebsocketApplicationHandler[] {
    return [...this._eventHandlers].filter(
      current => !!current.getHandlerMethod(event)
    );
  }

  private async _registerSocketHandlers(): Promise<void> {
    try {
      await Promise.all([
        ...[...this._eventHandlers].map(handler => handler.beforeWorkaroundSync())
      ]);
      await Promise.all([...this._eventHandlers].map(handler => handler.createStores()));
      this._runWithSyncedData();
    } catch (e) {
      this._log.error('Workaround WS sync failed ', e);
    }
    this._log.debug('Subscribing to message events');
    this._socket.on('deliver_message', message => {
      this._log.debug('An event from backend server received', message);
      if (message && message.status && message.eventName) {
        const eventName = message.eventName;
        const handlers: AbstractWebsocketApplicationHandler[] = this._getValidHandlers(eventName);
        if (handlers.length) {
          handlers.forEach(async handler => {
            try {
              await this._wsEventCacheService.saveEventData(message, message.value);
              await handler.execute(eventName, message.value);
            } catch (e) {
              this._log.error(`Handler ${handler.constructor.name} failed for eent ${eventName}`, e);
            }
          });
        } else {
          this._log.error('No handler for event ' + eventName, message);
        }
      } else {
        this._log.warn('Bad message from backend', message);
      }
    });

    this._socket.on('cache_clear', async () => {
      await this.clearCache();
    });
  }

  private _timeoutPromise(inputPromise: Promise<any>): Promise<any> {
    return Promise.race([
      inputPromise,
      new Promise(resolve => window.setTimeout(() => resolve('timeout'), 20000))
    ]);
  }

  private async _setupSync(response: { value: any[] }): Promise<void> {
    try {
      await this._loadingService.runWithLoading(async () => {
        await this._wsEventCacheService.createStores();
        await this._wsEventCacheService.setEventsInformation(response.value);
        await this._wsEventCacheService.createOfflineStores();
        await this._wsEventCacheService.applySync();
      });
    } catch (e) {
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
    await AsyncCollectionUtil.forEach([...this._eventHandlers], async handler => {
      const eventMap = handler.getEventsMap();
      await AsyncCollectionUtil.forEach(Object.keys(eventMap), async (event: keyof WebsocketSyncResponse) => {
        // Initial run with synced data
        if (this._wsEventCacheService.isSynchronizableEvent(event)) {
          this._log.debug(`Running handler for event ${event}, known to be in ${handler.constructor.name}`);
          try {
            const result = await this._timeoutPromise(
              handler.execute(event, await this._wsEventCacheService.findStoredValue(event))
            );
            if (result === 'timeout') {
              this._toastrService.warning(`Sync took too much for ${handler.constructor.name} on event ${event}`);
            }
          } catch (e) {
            this._toastrService.error(`Sync failed for ${handler.constructor.name} on event ${event}`);
            console.error(e);
          }
        }
      });
    });
  }
}
