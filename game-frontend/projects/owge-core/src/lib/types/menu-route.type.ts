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
     * Angular Router's route
     *
     * @since 0.7.0
     */
    path?: string;

    /**
     *
     * @since 0.7.0
     */
    text: string;

    /**
     * Action to run when you click the button
     *
     * @since 0.8.0
     */
    action?: Function;

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

    /**
     * If Internet connection is required to use this section
     *
     * @since 0.9.0
     */
    isConnectionRequired?: boolean;
}
