import { PipeTransform, Pipe } from '@angular/core';
import { Planet } from '@owge/universe';
import { MEDIA_ROUTES, Config } from '@owge/core';

/**
 * Displays the planet image
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 * @export
 */
@Pipe({
    name: 'planetImage'
})
export class PlanetImagePipe implements PipeTransform {
    public transform(planet: Planet): string {
        if (planet) {
            if (planet.specialLocation) {
                if (planet.specialLocation.imageUrl) {
                    return planet.specialLocation.imageUrl;
                } else {
                    return '/assets/img/special-location-without-icon.png';
                }
            } else if (Planet.isExplored(planet)) {
                return MEDIA_ROUTES.PLANET_RICHNESS_IMAGES + (planet.richness / 10) + Config.PLANET_RICHNESS_IMAGE_EXTENSION;
            } else {
                return MEDIA_ROUTES.PLANET_IMAGES + Config.PLANET_NOT_EXPLORED_IMAGE;
            }
        } else {
            return '';
        }
    }
}
