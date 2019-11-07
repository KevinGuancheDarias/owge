import { validImprovementType } from '../types/improvement-unit-type.type';
import { Improvement } from '../types/improvement.type';


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
        unitTypeId: number
    ): number {
        const retVal: number = improvement && improvement.unitTypesUpgrades && improvement.unitTypesUpgrades.length
            ? improvement.unitTypesUpgrades
                .filter(current => current.type === improvementType && current.unitTypeId === unitTypeId)
                .map(current => current.value)
                .reduce((sum, current) => sum + current, 0)
            : 0;
        return retVal;
    }

    private constructor() {
        // Util class doesn't have a constructor
    }
}
