import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { AbstractWebsocketApplicationHandler, LoggerHelper, DateUtil, StorageOfflineHelper } from '@owge/core';
import {
    TimeSpecial, UniverseGameService,
    ActiveTimeSpecialType,
    TimeSpecialStore,
    UniverseCacheManagerService,
    WsEventCacheService
} from '@owge/universe';

/**
 * Service to handle time special operations
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable()
export class TimeSpecialService extends AbstractWebsocketApplicationHandler {

    protected _log: LoggerHelper;

    private _timeSpecialStore: TimeSpecialStore = new TimeSpecialStore;

    public constructor(
        protected _universeGameService: UniverseGameService,
        private _wsEventCacheService: WsEventCacheService,
        private _universeCacheManagerService: UniverseCacheManagerService
    ) {
        super();
        this._eventsMap = {
            time_special_change: '_onTimeSpecialChange',
            time_special_unlocked_change: '_onTimeSpecialChange'
        };
    }

    /**
     * Returns the unlocked time specials <br>
     * Notice: The observable emits when the state of a timespecial changes (active, recharching, unlocked, etc)
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns {Observable<TimeSpecial[]>}
     */
    public findUnlocked(): Observable<TimeSpecial[]> {
        return this._timeSpecialStore.unlocked.asObservable();
    }

    /**
     * Activates a time special
     *
     * @todo When Feature/124 is done, the need to use reload improvements disappear,
     *  and TimeSpecials should register a listener for time_special status change
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param {number} timeSpecialId
     * @returns {Observable<ActiveTimeSpecialType>}
     */
    public activate(timeSpecialId: number): Observable<ActiveTimeSpecialType> {
        return this._universeGameService.requestWithAutorizationToContext(
            'game',
            'post',
            `time_special/activate`, timeSpecialId
        );
    }

    protected async _onTimeSpecialChange(content: TimeSpecial[]): Promise<void> {
        const contentOrEmpty = content || [];
        contentOrEmpty.forEach(current => {
            if (current.activeTimeSpecialDto) {
                current.activeTimeSpecialDto.pendingMillis = current.activeTimeSpecialDto.pendingMillis
                    ? current.activeTimeSpecialDto.pendingMillis
                    : current.activeTimeSpecialDto.pendingTime;
                if (!current.activeTimeSpecialDto.browserComputedTerminationDate) {
                    current.activeTimeSpecialDto = DateUtil.computeBrowserTerminationDate(current.activeTimeSpecialDto);
                }
            }
        });
        this._timeSpecialStore.unlocked.next(contentOrEmpty);
        this._wsEventCacheService.updateWithFrontendComputedData('time_special_change', content);
    }
}
