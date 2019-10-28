import { Injectable } from '@angular/core';
import * as io from 'socket.io-client';

import { LoggerHelper, ProgrammingError } from '@owge/core';

import { AbstractWebsocketApplicationHandler } from '../interfaces/abstract-websocket-application-handler';

@Injectable()
export class WebsocketService {

  private static readonly PROTOCOL_VERSION = '0.1.0';

  private _socket: SocketIOClient.Socket;
  private _isFirstConnection = true;
  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);
  private _credentialsToken: string;
  private _eventHandlers: AbstractWebsocketApplicationHandler[] = [];

  public addEventHandler(handler: AbstractWebsocketApplicationHandler) {
    this._eventHandlers.push(handler);
  }

  /**
   * Inits the websocket <br>
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {string} [targetUrl] Only required if connecting for the first time
   * @returns {Promise<void>} Solves when the socket is properly connected to the backend
   * @memberof WebsocketService
   */
  public initSocket(targetUrl?: string): Promise<void> {
    return new Promise<void>(resolve => {
      if (!this._socket) {
        if (!targetUrl) {
          throw new ProgrammingError('targetUrl MUST be specified at least in first executions');
        }
        this._log.debug('Connecting to remote websocket server', targetUrl);
        this._socket = io.connect(targetUrl);
        this._socket.on('connect', () => {
          if (this._isFirstConnection) {
            this._log.info('Connection established with success');
            resolve();
            this._isFirstConnection = false;
          }
        });
        this._socket.on('disconnect', () => this._log.info('client disconnected'));
        this._registerSocketHandlers();
      } else if (!this._socket.connected) {
        this._log.debug('Reconnecting to specified server');
        this._socket.on('connect', () => {
          this._log.debug('Reconnected with success');
          resolve();
        });
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

  public async authenticate(userJwtToken?: string): Promise<void> {
    this.setAuthenticationToken(userJwtToken);
    await this.initSocket();
    this._log.debug('starting authentication');
    return await new Promise<void>((resolve, reject) => {
      this._socket.emit('authentication', JSON.stringify({
        value: this._credentialsToken,
        protocol: WebsocketService.PROTOCOL_VERSION,
      }));
      this._socket.on('authentication', response => {
        this._log.debug('authentication attemp response is', response);
        if (response.status === 'ok') {
          this._log.debug('authenticated succeeded');
          resolve();
        } else {
          this._log.warn('An error occuring while trying to authenticate, response was', response);
          reject(response);
        }
      });
    });
  }

  private _registerSocketHandlers(): void {
    this._socket.on('deliver_message', message => {
      this._log.debug('An event from backend server received', message);
      if (message && message.status && message.status.eventName) {
        const eventName = message.status.eventName;
        const handler: AbstractWebsocketApplicationHandler = this._eventHandlers.find(current => !!current.getHandlerMethod(eventName));
        if (handler) {
          handler.execute(this._socket, eventName, message.content);
        } else {
          this._log.error('No handler for event ' + eventName, message);
        }
      } else {
        this._log.warn('Bad message from backend', message);
      }
    });
  }
}
