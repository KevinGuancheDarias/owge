export type messageStatus = 'ok' | 'error';

/**
 * Represents a websocket message sent by a client
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface WebsocketMesage
 */
export interface WebsocketMesage {
    status: messageStatus;

    /**
     * Value of message, if <i>status</i> is error, will be an error string
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @type {*}
     * @memberof WebsocketMesage
     */
    value?: any;

    /**
     * Protocol version <br>
     * Matchs this project version, for example 0.1.0
     *
     * @type {string}
     * @memberof WebsocketMesage
     */
    protocol?: string;
}
