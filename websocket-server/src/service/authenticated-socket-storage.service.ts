import { UserNotFoundError } from '../shared/exception/user-not-found.error';
import { validEvents, WebsocketClient } from '../shared/types/websocket-client.type';
import { ProgrammingError } from '../shared/exception/programming.error';
import { User } from '../shared/types/user.type';
import { WebsocketMesage } from '../shared/types/websocket-message.type';
import { DeliveryMessageContainer } from '../shared/types/delivery-message-container.type';

/**
 * Used to manage authenticated sockets
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @class AuthenticatedSocketStorageService
 */
export class AuthenticatedSocketStorageService {
    private _storage: { [key: string]: WebsocketClient[] } = {};

    /**
     * Adds a client to the local storage
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {WebsocketClient} client
     * @throws {ProgrammingError} If the user is not defined in the socket
     * @memberof AuthenticatedSocketStorageService
     */
    public addClient(client: WebsocketClient): void {
        if (!client.user) {
            throw new ProgrammingError('User MUST be logged in, before storing in the AuthenticatedSocketStorageService');
        }
        this._doStore(client);
    }

    /**
     * Emits a socket message to all websockets owned by the user
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {User} user User that owns the sockets
     * @param {validEvents} event Event name to emit
     * @param {WebsocketMessage} message Message to emit
     * @throws {UserNotFoundError} If user doesn't have any open socket
     * @memberof AuthenticatedSocketStorageService
     */
    public emitToUser(user: User, event: validEvents, message: WebsocketMesage): void {
        const userSockets: WebsocketClient[] = this._storage[user.id];
        if (!userSockets) {
            throw new UserNotFoundError('No sockets for user ' + user.id);
        }
        userSockets.forEach(current => current.emit(event, message));
    }

    public deliverToUser(message: DeliveryMessageContainer): number {
        const sockets: WebsocketClient[] = this._forUserSocket(message.targetUser, socket => socket.emit('deliver_message', message));
        return sockets
            ? sockets.length
            : 0;
    }

    /**
     * Stores the client in the <i>_storage</i>
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @private
     * @param {WebsocketClient} client
     * @memberof AuthenticatedSocketStorageService
     */
    private _doStore(client: WebsocketClient): void {
        if (this._storage[client.user.id] instanceof Array) {
            this._storage[client.user.id].push(client);
        } else {
            this._storage[client.user.id] = [client];
        }
    }

    /**
     * Executes the passed function foreach user connected socket
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @private
     * @param {User} user target user
     * @param {(currentSocket: WebsocketClient) => void} fn function to execute, receives the current socket as argument
     * @returns {WebsocketClient[]} Returns the array of sockets for target user
     * @memberof AuthenticatedSocketStorageService
     */
    private _forUserSocket(user: User, fn: (currentSocket: WebsocketClient) => void): WebsocketClient[] {
        const userSockets: WebsocketClient[] = this._storage[user.id];
        if (userSockets) {
            userSockets.forEach(current => fn(current));
        }
        return userSockets;
    }
}
