import { Injectable } from '@angular/core';
import { AbstractWebsocketApplicationHandler } from '../interfaces/abstract-websocket-application-handler';
import { ThemeService } from './theme.service';
import { ToastrService } from './toastr.service';

@Injectable()
export class WarningWebsocketApplicationHandlerService extends AbstractWebsocketApplicationHandler {

    #currentTheme: string;

    constructor(private _toastrService: ToastrService, themeService: ThemeService) {
        super();
        this._eventsMap.warn_message = 'warnMessage';
        themeService.currentTheme$.subscribe(theme => this.#currentTheme = theme);
    }

    public warnMessage(content: string): void {
        this._log.debug('Warn from server, will play audio, for trolling purpose');
        this._toastrService.warn(content);
        new Audio(`/theme/assets/${this.#currentTheme}/audio/notification.mp3`).play();
    }
}
