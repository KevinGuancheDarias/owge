
/**
 * Represents a "Menu button"
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @interface MenuRoute
 */
export interface MenuRoute {


    /**
     *
     * @since 0.7.0
     * @type {string}
     * @memberof MenuRoute
     */
    path: string;


    /**
     *
     * @since 0.7.0
     * @type {string}
     * @memberof MenuRoute
     */
    text: string;


    /**
     *
     *
     * @type {string}
     * @since 0.7.0
     * @memberof MenuRoute
     */
    icon?: string;

    /**
     * Action to run to check if should display the route or not
     *
     * @since 0.7.0
     * @memberof MenuRoute
     */
    ngIf?: () => Promise<boolean>;

    /**
     *
     * @type {boolean}
     * @since 0.7.0
     */
    isNgIfOk?: boolean;
}
