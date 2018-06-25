import { MEDIA_ROUTES, Config } from './../config/config.pojo';
export class PlanetPojo {
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

    public static findImage(planet: PlanetPojo): string {
        if (planet.specialLocationId) {
            throw new Error('Unsupported feature for now');
        } else if (PlanetPojo.isExplored(planet)) {
            return MEDIA_ROUTES.PLANET_RICHNESS_IMAGES + (planet.richness / 10) + Config.PLANET_RICHNESS_IMAGE_EXTENSION;
        } else {
            return MEDIA_ROUTES.PLANET_IMAGES + Config.PLANET_NOT_EXPLORED_IMAGE;
        }
    }

    /**
     * Detects if the planet is explored or not
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @static
     * @param {PlanetPojo} planet
     * @returns {boolean}
     * @memberof PlanetPojo
     */
    public static isExplored(planet: PlanetPojo): boolean {
        return planet.richness !== null;
    }
}
