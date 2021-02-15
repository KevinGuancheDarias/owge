import { Injectable } from '@angular/core';
import { AbstractWebsocketApplicationHandler } from '@owge/core';
import { Observable } from 'rxjs';
import { SystemMessageStore } from '../storages/system-message.store';
import { SystemMessage } from '../types/system-message.type';
import { UniverseGameService } from './universe-game.service';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.16
 * @export
 */
@Injectable()
export class SystemMessageService extends AbstractWebsocketApplicationHandler {
    private _store: SystemMessageStore = new SystemMessageStore();

    public constructor(private _universeGameService: UniverseGameService) {
        super();
        this._eventsMap = {
            system_message_change: '_onSystemMessageChange'
        };
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.16
     * @returns {Observable<SystemMessage[]>}
     */
    public findAll(): Observable<SystemMessage[]> {
        return this._store.messages.asObservable();
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.16
     * @param {SystemMessage[]} messages
     * @returns {Observable<void>}
     */
    public markAsRead(messages: SystemMessage[]): Observable<void> {
        return this._universeGameService.requestWithAutorizationToContext(
            'game',
            'post',
            'system-message/mark-as-read',
            messages.map(message => message.id)
        );
    }

    protected _onSystemMessageChange(content: SystemMessage[]) {
        this._store.messages.next(content);
    }
}
