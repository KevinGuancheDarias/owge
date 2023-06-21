import {Planet} from '@owge/universe';
import {NavigationConfig} from '../types/navigation-config.type';

export class PlanetUtil {
    static planetToNavigationConfig(planet: Planet): NavigationConfig {
        return {
            galaxy: planet.galaxyId,
            sector: planet.sector,
            quadrant: planet.quadrant,
            planetId: planet.id
        };
    }
}
