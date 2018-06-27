import { Universe } from '../../shared-pojo/universe.pojo';


/**
 * Represents the credentials (usually sent from to different frontend domains)
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface Credentials
 */
export interface Credentials {

    /**
     * RAW authentication token
     *
     * @type {string}
     * @memberof Credentials
     */
    rawToken: string;

    selectedUniverse: Universe;
}
