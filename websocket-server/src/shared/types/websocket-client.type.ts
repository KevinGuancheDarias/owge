import { WebsocketMesage } from './websocket-message.type';
import { User } from './user.type';
import { DeliveryMessageContainer } from './delivery-message-container.type';

export type validEvents = 'authentication' | 'deliver_message' | 'websocket_server_ack' | 'deliver_to_client'
    | 'web_browser_ack' | 'no_client_socket';

/**
 * Represents a connected client
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface WebsocketClient
 */
export interface WebsocketClient extends SocketIO.Socket {

    /**
     * Represents the user id, may be null, if not authenticated
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @type {number}
     * @memberof WebsocketClient
     */
    user?: User;

    /**
     * JWT encoded token
     *
     * @type {string}
     * @memberof WebsocketClient
     */
    token?: string;

    /**
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @type {Date}
     * @memberof WebsocketClient
     */
    lastAction?: Date;

    on(event: validEvents, listener: (message: string | DeliveryMessageContainer) => void): this;

    emit(event: validEvents, value: string | WebsocketMesage | DeliveryMessageContainer): boolean;
}
