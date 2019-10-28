import { Injector } from '@angular/core';

/**
 * Represents a "Menu button"
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
export interface MenuRoute {

    /**
     *
     * @since 0.7.0
     */
    path: string;


    /**
     *
     * @since 0.7.0
     */
    text: string;


    /**
     *
     *
     * @since 0.7.0
     */
    icon?: string;

    /**
     * Action to run to check if should display the route or not
     *
     * @since 0.7.0
     */
    ngIf?: (injector: Injector) => Promise<boolean>;

    /**
     *
     * @since 0.7.0
     */
    isNgIfOk?: boolean;
}
