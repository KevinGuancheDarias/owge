
/**
 * Represents a faction
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface Faction {
    id: number;
    hidden: boolean;
    name: string;
    image: number;
    imageUrl: string;
    description: string;
    primaryResourceName: string;
    primaryResourceImage: number;
    primaryResourceImageUrl: string;
    secondaryResourceName: string;
    secondaryResourceImage: number;
    secondaryResourceImageUrl: string;
    energyName: string;
    energyImage: number;
    energyImageUrl: string;
    initialPrimaryResource: number;
    initialSecondaryResource: number;
    initialEnergy: number;
    primaryResourceProduction: number;
    secondaryResourceProduction: number;
    maxPlanets: number;

    /**
     * @since 0.10.0
     */
    customPrimaryGatherPercentage: number;

    /**
     * @since 0.10.0
     */
    customSecondaryGatherPercentage: number;
}
