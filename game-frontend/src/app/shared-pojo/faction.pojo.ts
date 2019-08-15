
/**
 *
 * @deprecated As of 0.8.0 it's better to use FactionModule/Faction
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export class Faction {
    public id: number;
    public hidden: boolean;
    public name: string;
    public image: string;
    public description: string;
    public primaryResourceName: string;
    public primaryResourceImage: string;
    public secondaryResourceName: string;
    public secondaryResourceImage: string;
    public energyName: string;
    public energyImage: string;
    public initialPrimaryResource: number;
    public initialSecondaryResource: number;
    public initialEnergy: number;
    public primaryResourceProduction: number;
    public secondaryResourceProduction: number;

}
