import { validContext } from '../services/core-http.service';

/**
 * Configuration of the account server <br>
 * <b>NOTICE:</b> May be defined as a provider, this is the reason of it to be a class instead of a interface
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export class OwgeCoreConfig {
    public url: string;
    public contextPath: validContext;
    public loginEndpoint: string;
    public loginDomain: string;
    public loginClientId?: string;
    public loginClientSecret?: string;
}
