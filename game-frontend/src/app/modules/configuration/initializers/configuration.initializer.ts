import { Provider, APP_INITIALIZER } from '@angular/core';
import { ConfigurationService } from '../services/configuration.service';
import { UniverseService } from '../../universe/services/universe.service';



/**
 * Initializes the ConfigurationService
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.4
 * @export
 * @param {ConfigurationService} configurationService
 * @param {UniverseService} universeService
 * @returns
 */
export function initConfiguration(configurationService: ConfigurationService, universeService: UniverseService) {
    return async (): Promise<void> => {
        if (universeService.getSelectedUniverse()) {
            await configurationService.init().catch(() => alert('FATAL, could not get universe configuration'));
        }
    };
}

export const ConfigurationInitializer: Provider = {
    provide: APP_INITIALIZER,
    useFactory: initConfiguration,
    deps: [ConfigurationService, UniverseService],
    multi: true
};
