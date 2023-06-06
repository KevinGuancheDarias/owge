
/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface SpecialLocation {
    id: number;
    name: string;
    description: string;
    image?: number;
    imageUrl?: string;
    galaxyId: number;
    galaxyName: string;
    assignedPlanetId: number;
    assignedPlanetName: string;
    improvement?: any;
}
