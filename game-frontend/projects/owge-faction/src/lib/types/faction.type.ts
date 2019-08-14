
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
    image: string;
    description: string;
    primaryResourceName: string;
    primaryResourceImage: string;
    secondaryResourceName: string;
    secondaryResourceImage: string;
    energyName: string;
    energyImage: string;
    initialPrimaryResource: number;
    initialSecondaryResource: number;
    initialEnergy: number;
    primaryResourceProduction: number;
    secondaryResourceProduction: number;

}
