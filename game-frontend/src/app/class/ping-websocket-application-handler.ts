import { AbstractWebsocketApplicationHandler } from '../interfaces/abstract-websocket-application-handler';

export class PingWebsocketApplicationHandler extends AbstractWebsocketApplicationHandler {

    constructor() {
        super();
        this._eventsMap.ping = 'handlePing';
    }

    public handlePing(): void {
        this._log.debug('Ping event from server, will play audio');
        new Audio('/assets/audio/notification.mp3').play();
    }
}
