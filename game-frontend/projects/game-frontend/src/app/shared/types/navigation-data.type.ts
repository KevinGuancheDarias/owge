import { Planet } from '@owge/types/universe';
import { Galaxy } from '@owge/types/galaxy';

export interface NavigationData {
    galaxies: Galaxy[];
    planets: Planet[];
}
