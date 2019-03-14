import { UniverseService } from '../services/universe.service';
import { APP_INITIALIZER, Provider } from '@angular/core';


/**
 * Initializes UniverseService
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.3
 * @export
 * @param {UniverseService} universeService
 * @returns
 */
export function initUniverse(universeService: UniverseService) {
    return (): void => universeService.init();
}

export const UniverseInitializer: Provider = {
    provide: APP_INITIALIZER,
    useFactory: initUniverse,
    deps: [UniverseService],
    multi: true
};
