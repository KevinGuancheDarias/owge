import { AbstractWebsocketApplicationHandler, ThemeService } from '@owge/core';

export class PingWebsocketApplicationHandler extends AbstractWebsocketApplicationHandler {

    #currentTheme: string;

    constructor(themeService: ThemeService) {
        super();
        this._eventsMap.ping = 'handlePing';
        themeService.currentTheme$.subscribe(theme => this.#currentTheme = theme);
    }

    public handlePing(): void {
        this._log.debug('Ping event from server, will play audio');
        new Audio(`/theme/assets/${this.#currentTheme}/audio/notification.mp3`).play();
    }
}
