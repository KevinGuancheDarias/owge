import { User } from './user.type';
import { WebsocketMessageStatus } from './websocket-message-status.type';

/**
 * Represents a type that is like the backend result of <i>SocketIoService.deliverMessage()</i>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @interface DeliveryMessageContainer
 */
export interface DeliveryMessageContainer {

    /**
     * Represents the status of the delivery process
     *
     * @type {WebsocketMessageStatus}
     * @memberof DeliveryMessageContainer
     */
    status: WebsocketMessageStatus;

    /**
     * Represents the user that sends the message <br >
     * Can be undefined if the sender is the system
     *
     * @type {User}
     * @memberof WebsocketMesage
     */
    sourceUser?: User;

    /**
     * Represents the target user of the websocket message
     *
     * @type {User}
     * @memberof WebsocketMesage
     */
    targetUser?: User;

    /**
     * Content send as data (it's sent by backend server), can have any structure <br>
     * Type will be redefined in frontend logic
     *
     * @type {any}
     * @memberof DeliveryMessageContainer
     */
    content?: any;

    /**
     * Client data, it's defined by user browser
     *
     * @type {*}
     * @memberof DeliveryMessageContainer
     */
    clientData?: any;
}
