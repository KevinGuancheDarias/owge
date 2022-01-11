import { IdName } from './id-name.type';

/**
 * Represents a common entity in the OWGE, most entities have id, name and <b>optionally</b> a description
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface CommonEntity<K> extends IdName<K> {
    description?: string;
}
