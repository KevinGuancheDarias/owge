import { MEDIA_ROUTES, Config } from '@owge/core';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export class Planet {
    public id: number;
    public name: string;
    public galaxyId: number;
    public galaxyName: string;
    public sector: number;
    public quadrant: number;
    public planetNumber: number;
    public ownerId: number;
    public richness: number;
    public home: boolean;
    public specialLocationId: number;

    public static findImage(planet: Planet): string {
        if (planet.specialLocationId) {
            throw new Error('Unsupported feature for now');
        } else if (Planet.isExplored(planet)) {
            return MEDIA_ROUTES.PLANET_RICHNESS_IMAGES + (planet.richness / 10) + Config.PLANET_RICHNESS_IMAGE_EXTENSION;
        } else {
            return MEDIA_ROUTES.PLANET_IMAGES + Config.PLANET_NOT_EXPLORED_IMAGE;
        }
    }

    /**
     * Detects if the planet is explored or not
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param  planet
     * @returns
     */
    public static isExplored(planet: Planet): boolean {
        return planet && typeof planet.richness !== 'undefined';
    }
}
