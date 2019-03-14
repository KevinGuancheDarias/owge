import { Provider, APP_INITIALIZER } from '@angular/core';

import { ClockSyncService } from '../services/clock-sync.service';
import { UniverseService } from '../../universe/services/universe.service';


/**
 * Initializes the clockSyncservice
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.3
 * @export
 * @param {ClockSyncService} clockSyncService
 * @param {UniverseService} universeService
 * @returns
 */
export function initClock(clockSyncService: ClockSyncService, universeService: UniverseService) {
    return async (): Promise<void> => {
        if (universeService.getSelectedUniverse()) {
            await clockSyncService.init().catch(() => alert('FATAL, could not get server time'));
        }
    };
}

export const ClockInitializer: Provider = {
    provide: APP_INITIALIZER,
    useFactory: initClock,
    deps: [ClockSyncService, UniverseService],
    multi: true
};
