import { Injectable } from '@angular/core';
import { AbstractWebsocketApplicationHandler } from '@owge/core';
import { UniverseGameService } from '@owge/universe';
import { Observable } from 'rxjs';
import { TwitchStore } from '../store/twitch.store';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.5
 * @export
 */
@Injectable()
export class TwitchService extends AbstractWebsocketApplicationHandler {
    private _store: TwitchStore = new TwitchStore;

    public constructor(private _universeGameService: UniverseGameService) {
        super();
        this._eventsMap = {
            twitch_state_change: '_onTwitchStateChange'
        };
    }

    public async workaroundSync(): Promise<void> {
        this._onTwitchStateChange(
            await this._universeGameService.requestWithAutorizationToContext('game', 'get', 'twitch-state').toPromise()
        );
    }

    /**
     * Defines if game developer is live or not
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.5
     * @param state
     */
    public defineTwitchState(state: boolean): Observable<void> {
        return this._universeGameService.requestWithAutorizationToContext('game', 'put', 'twitch-state', state);
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.5
     * @returns
     */
    public state(): Observable<boolean> {
        return this._store.state.asObservable();
    }

    protected _onTwitchStateChange(value: boolean): void {
        this._store.state.next(value);
    }

}
