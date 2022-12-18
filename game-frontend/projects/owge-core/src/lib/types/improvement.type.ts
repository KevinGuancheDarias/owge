import { ImprovementUnitType } from './improvement-unit-type.type';

/**
 * Represents an improvement
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface Improvement {
    id: number;
    moreSoldiersProduction: number;
    morePrimaryResourceProduction: number;
    moreSecondaryResourceProduction: number;
    moreEnergyProduction: number;
    moreChargeCapacity: number;
    moreMissions: number;
    moreUpgradeResearchSpeed: number;
    moreUnitBuildSpeed: number;
    unitTypesUpgrades?: ImprovementUnitType[];
}
