import { PlanetPojo } from '../../shared-pojo/planet.pojo';
import { Galaxy } from '../pojos/galaxy.pojo';

export interface NavigationData {
    galaxies: Galaxy[];
    planets: PlanetPojo[];
}
