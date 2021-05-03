import { Injectable } from '@angular/core';
import { AbstractWebsocketApplicationHandler } from '../interfaces/abstract-websocket-application-handler';
import { ToastrService } from './toastr.service';

@Injectable()
export class WarningWebsocketApplicationHandlerService extends AbstractWebsocketApplicationHandler {

    constructor(private _toastrService: ToastrService) {
        super();
        this._eventsMap.warn_message = 'warnMessage';
    }

    public warnMessage(content: string): void {
        this._log.debug('Warn from server, will play audio, for trolling purpose');
        this._toastrService.warn(content);
        new Audio('/assets/audio/notification.mp3').play();
    }
}
