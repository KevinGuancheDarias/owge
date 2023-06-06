import { MenuRoute } from './menu-route.type';

/**
 * Represents the data sent to the <i>RouterRootComponent</i>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
export interface RouterData {
    sectionTitle: string;


    /**
     * The default route to load, when none specified
     *
     * @since 0.8.1
     */
    default: string;
    routes: MenuRoute[];
}
