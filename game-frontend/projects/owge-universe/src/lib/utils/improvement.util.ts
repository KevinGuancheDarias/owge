import { Improvement, UnitType, validImprovementType } from '@owge/core';

/**
 * Has methods to interact with improvements
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export class ImprovementUtil {

    /**
     * Finds all unit type improvements of the same type for one target unit type id
     *
     * @see To understand why I created a temporary normally useless variable,
     *  see: https://github.com/ng-packagr/ng-packagr/issues/696#issuecomment-387114613
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param  improvement
     * @param  improvementType
     * @param  unitTypeId
     * @returns
     */
    public static findUnitTypeImprovement(
        improvement: Improvement,
        improvementType: validImprovementType,
        unitType: UnitType
    ): number {
        let retVal = 0;
        if (improvement && improvement.unitTypesUpgrades && improvement.unitTypesUpgrades.length) {
            retVal = improvement.unitTypesUpgrades
                .filter(current => current.type === improvementType && current.unitType.id === unitType.id)
                .map(current => current.value)
                .reduce((sum, current) => sum + current, 0);
            if (unitType && unitType.hasToInheritImprovements && unitType.parent) {
                retVal += ImprovementUtil.findUnitTypeImprovement(improvement, improvementType, unitType.parent);
            }
        }
        return retVal;
    }

    private constructor() {
        // Util class doesn't have a constructor
    }
}
