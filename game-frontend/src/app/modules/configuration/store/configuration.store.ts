import { Injectable } from '@angular/core';
import { ReplaySubject } from 'rxjs';
import { Configuration } from '../types/configuration.type';

/**
 * Handles the configuration
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.5
 * @export
 * @class ConfigurationStore
 */
@Injectable()
export class ConfigurationStore {
    public readonly  currentConfiguration: ReplaySubject<Configuration<any>[]> = new ReplaySubject(1);
}
