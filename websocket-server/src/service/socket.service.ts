import { ProtocolVersion } from '../shared/class/protocol-version.class';
import * as io from 'socket.io';

import { WebsocketClient, validEvents } from '../shared/types/websocket-client.type';
import { ConfigurationService } from './configuration.service';
import { WebsocketMesage } from '../shared/types/websocket-message.type';
import { AuthenticationService } from './authenticatio.service';
import { AuthenticatedSocketStorageService } from './authenticated-socket-storage.service';
import { User } from '../shared/types/user.type';
import { ProtocolVersionProperties } from '../shared/types/protocol-version-properties.type';
import { DeliveryMessageContainer } from '../shared/types/delivery-message-container.type';
import { Log } from 'ng2-logger';

export class SocketService {

    /**
     * Version of the running API, matchs the project ID <br>
     * The client MUST connect to the same version
     *
     * @private
     * @static
     * @memberof SocketService
     */
    private static readonly PROTOCOL_VERSION: ProtocolVersionProperties = {
        major: 0,
        minor: 1,
        patch: 0
    };

    /**
     * Number of seconds to wait, before disconnecting unauthenticated socket
     *
     * @private
     * @static
     * @type {number}
     * @memberof SocketService
     */
    private static readonly AUTHENTICATION_TIMEOUT = 30;

    private _io: SocketIO.Server;
    private _nonAuthenticatedConnections: WebsocketClient[] = [];
    private _systemSockets: WebsocketClient[];
    private _log = Log.create(this.constructor.name);

    /**
     * Creates an instance of SocketService.
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {ConfigurationService} configurationService the information to connect to the database
     * @memberof SocketService
     */
    public constructor(
        private _authenticationService: AuthenticationService,
        private _authenticatedSocketStorageService: AuthenticatedSocketStorageService,
        configurationService: ConfigurationService
    ) {
        configurationService.findJwtSecret().then(secret => {
            this._log.d('constructor', 'jwt secret obtained with success');
            this._authenticationService.setValidaitonSecret(secret);
            this._io = io();
            this._handleOnConnection();
            this._registerDeathUnauthenticatedSocketsCleaner();
            this._io.listen(process.env.websocketPort || 3000);
            this._log.d('constructor', 'Server listening in port 3000');
        });
    }

    private _handleOnConnection() {
        this._io.on('connection', client => {
            this._log.d('_handleOnConnection', 'someone is connected');
            this._nonAuthenticatedConnections.push(this._updateClientLastModified(client));
            this._handleAuthentication(client);
        });
    }

    private _updateClientLastModified(client: WebsocketClient): WebsocketClient {
        client.lastAction = new Date();
        return client;
    }

    private _registerDeathUnauthenticatedSocketsCleaner(): void {
        setInterval(() => {
            this._nonAuthenticatedConnections = this._nonAuthenticatedConnections.map(current => {
                const nowTime: number = (new Date().getTime()) / 1000;
                const targetTime: number = current.lastAction.getTime() / 1000;
                if ((nowTime - targetTime) >= SocketService.AUTHENTICATION_TIMEOUT) {
                    current.disconnect(true);
                    return null;
                } else {
                    return current;
                }
            }).filter(current => current !== null);
        }, SocketService.AUTHENTICATION_TIMEOUT);
    }

    private _emitError(client: WebsocketClient, eventName: validEvents, message: string): boolean {
        return client.emit(eventName, {
            status: 'error',
            value: message
        });
    }

    private _handleSystemActions(socket: WebsocketClient): void {
        socket.on('deliver_message', message => {
            this._log.d('_handleSystemActions()', 'Backend whish to deliver a message', message);
            socket.emit('websocket_server_ack', message);
        });
        socket.on('deliver_to_client', message => {
            const parsedMessage = <DeliveryMessageContainer>message;
            if (this._authenticatedSocketStorageService.deliverToUser(parsedMessage)) {
                this._log.d(
                    '_handleSystemActions()',
                    ' Message was send to client browser with success, messageId =',
                    parsedMessage.status.id
                );
                socket.emit('web_browser_ack', parsedMessage);
            } else {
                this._log.d('_handleSystemActions()', 'Could not delive message ' + parsedMessage.status.id + ' no client connected');
                socket.emit('no_client_socket', parsedMessage);
            }
        });
    }

    /**
     * Handles the authentication request from the client or a system
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @private
     * @param {WebsocketClient} client Socket that requested authentication
     * @returns {this}
     * @memberof SocketService
     */
    private _handleAuthentication(client: WebsocketClient): this {
        client.on('authentication', message => {
            this._log.d('_handleAuthentication()', 'client sent authentication message ', message);
            const parsedMessage: WebsocketMesage = JSON.parse(<any>message);
            if (!this._isSupportedProtocolVersion(parsedMessage)) {
                this._log.w('_handleAuthentication()', 'Invalid protocol version specified by client');
                this._emitError(client, 'authentication', 'Unsupported version, server runs '
                    + ProtocolVersion.convertPropertiesToString(SocketService.PROTOCOL_VERSION));
            } else if (this._authenticationService.isValid(parsedMessage.value)) {
                this._nonAuthenticatedConnections = this._nonAuthenticatedConnections.filter(current => current.id !== client.id);
                client.user = this._authenticationService.findTokenUser(parsedMessage.value);
                this._log.d('_handleAuthentication()', 'User identified as ' + client.user);
                this._registerAuthenticatedSocket(client);
                client.emit('authentication', { status: 'ok' });
            } else {
                this._log.i('_handleAuthentication()', 'User supplied bad credentials');
                this._emitError(client, 'authentication', 'Bad credentials, invalid session');
            }
        });
        return this;
    }

    private _registerAuthenticatedSocket(socket: WebsocketClient): void {
        if (this._isSystem(socket.user)) {
            this._log.d('_registerAuthenticatedSocket()', 'Is a system socket');
            this._addSystemSocket(socket);
            this._handleSystemActions(socket);
        } else {
            this._log.d('_registerAuthenticatedSocket()', 'Is a user socket with id ' + socket.user.id);
            this._authenticatedSocketStorageService.addClient(socket);
        }
    }

    private _isSystem(user: User) {
        return user.id === 0 && user.username === 'system';
    }

    private _addSystemSocket(socket: WebsocketClient): void {
        if (this._systemSockets instanceof Array) {
            this._systemSockets.push(socket);
        } else {
            this._systemSockets = [socket];
        }
    }

    /**
     * Checks if the version is supported, changes to the patch level are accepted
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @private
     * @param {WebsocketMesage} message
     * @returns {boolean}
     * @memberof SocketService
     */
    private _isSupportedProtocolVersion(message: WebsocketMesage): boolean {
        if (ProtocolVersion.isValidVersion(message.protocol)) {
            const protocolVersion: ProtocolVersion = ProtocolVersion.getInstance(message.protocol);
            return !protocolVersion.isMajorDifferentThan(SocketService.PROTOCOL_VERSION)
                && !protocolVersion.isMinorDifferentThan(SocketService.PROTOCOL_VERSION);
        } else {
            return false;
        }

    }
}
