import { Injectable } from '@angular/core';

/**
 * Has methods to interact with the config of a localStorage entry
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 * @export
 */
@Injectable()
export class LocalConfigurationService {

    /**
     * Finds the config by given <i>store</i>
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     * @template T The type to return (should be a POJO)
     * @param store The key, tipically the constructor name of the class
     * @returns The stored data as POJO
     */
    public findConfig<T = any>(store: string): T {
        return JSON.parse(localStorage.getItem(store));
    }

    /**
     * Saves the config to the localStorage
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     * @param store The key, tipically the constructor name of the class
     * @param dataObject The data to save (will be JSON.stringified)
     * @returns
     */
    public saveConfig(store: string, dataObject: any): void {
        return localStorage.setItem(store, JSON.stringify(dataObject));
    }
}
