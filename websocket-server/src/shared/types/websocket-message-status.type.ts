/**
 * Typescript equivalent of the same class in the backend implementation
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface WebsocketMessageStatus
 */
export interface WebsocketMessageStatus {
    id: number;
    eventName: string;
    unwhilingToDelivery: boolean;
    socketServerAck: boolean;
    socketNotFound: boolean;
    webBrowserAck: boolean;
    isUserAckRequired: boolean;
}
