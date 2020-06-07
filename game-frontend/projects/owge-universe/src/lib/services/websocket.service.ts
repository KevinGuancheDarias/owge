import { Injectable } from '@angular/core';
import * as io from 'socket.io-client';
import { ToastrService } from 'ngx-toastr';

import { Observable, Subject, ReplaySubject } from 'rxjs';
import { WsEventCacheService } from './ws-event-cache.service';
import {
  LoggerHelper, AbstractWebsocketApplicationHandler, ProgrammingError, SessionStore,
  SessionService, LoadingService
} from '@owge/core';

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
  private _socket: SocketIOClient.Socket;
  private _isFirstConnection = true;
  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);
  private _credentialsToken: string;
  private _eventHandlers: AbstractWebsocketApplicationHandler[] = [];
  private _isAuthenticated = false;
  private _isConnected: Subject<boolean> = new ReplaySubject(1);
  private _hasTriggeredFirtsOffline = false;
  private _isWantedDisconnection: boolean;

  public constructor(
    private _wsEventCacheService: WsEventCacheService,
    private _sessionService: SessionService,
    private _toastrService: ToastrService,
    private _loadingService: LoadingService,
    sessionStore: SessionStore
  ) {
    this._isConnected.next(false);
    this._isConnected.subscribe(sessionStore.isConnected.next.bind(sessionStore.isConnected));
  }

  public addEventHandler(...handler: AbstractWebsocketApplicationHandler[]) {
    this._eventHandlers = this._eventHandlers.concat(handler);
  }

  /**
   * Preprends to the beggining
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param handler
   */
  public preprendEventHandler(handler: AbstractWebsocketApplicationHandler): void {
    this._eventHandlers = [handler, ...this._eventHandlers];
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
            await Promise.all(this._eventHandlers.map(current => current.workaroundInitialOffline()));
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
        this._socket.on('authentication', response => {
          this._socket.removeEventListener('authentication');
          if (response.status === 'ok') {
            this._log.debug('authenticated succeeded');
            this._wsEventCacheService.setEventsInformation(response.value);
            this._isAuthenticated = true;
            this._registerSocketHandlers();
            resolve();
          } else if (response.value === 'Invalid Credentails') {
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

  private async _registerSocketHandlers(): Promise<void> {
    try {
      this._log.debug('Invoking workaroundSync');
      await this._loadingService.addPromise(Promise.all(this._eventHandlers.map(async current => {
        const result = await this._timeoutPromise(current.workaroundSync());
        if (result === 'timeout') {
          const errorMsg = `${current.constructor.name}.workaroundSync ()timed out`;
          this._log.error(errorMsg);
          this._toastrService.error(errorMsg);
        }
        return result;
      })));
    } catch (e) {
      this._log.error('Workaround WS sync failed ', e);
    }
    this._log.debug('Subscribing to message events');
    this._socket.on('deliver_message', message => {
      this._log.debug('An event from backend server received', message);
      if (message && message.status && message.eventName) {
        const eventName = message.eventName;
        const handlers: AbstractWebsocketApplicationHandler[] = this._eventHandlers.filter(
          current => !!current.getHandlerMethod(eventName)
        );
        if (handlers.length) {
          handlers.forEach(async handler => {
            try {
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
  }

  private _timeoutPromise(inputPromise: Promise<any>): Promise<any> {
    return Promise.race([
      inputPromise,
      new Promise(resolve => window.setTimeout(() => resolve('timeout'), 3000))
    ]);
  }
}
