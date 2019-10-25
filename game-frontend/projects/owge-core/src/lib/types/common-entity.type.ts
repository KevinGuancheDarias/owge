
/**
 * Represents a common entity in the OWGE, most entities have id, name and description
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface CommonEntity<K> {
    id: K;
    name: string;
    description: string;
}
