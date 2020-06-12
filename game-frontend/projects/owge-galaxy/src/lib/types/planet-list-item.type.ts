import { Planet } from '@owge/universe';

/**
 * Represents an item of the uset planet list
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface PlanetListItem {
    userId: number;
    username: string;
    planet: Planet;
    name: string;
}
