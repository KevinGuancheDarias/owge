import { UnitType } from './unit-type.type';

/**
 * @internal
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
export type validImprovementType = 'ATTACK' | 'DEFENSE' | 'SHIELD' | 'SPEED' | 'AMOUNT';
/**
 * Represents an improvement Unit Type
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface ImprovementUnitType {
    id?: number;
    type: validImprovementType;

    unitType: UnitType;

    /**
     * @deprecated since 0.9.0
     */
    unitTypeId: number;

    /**
     * @deprecated since 0.9.0
     */
    unitTypeName?: string;
    value: number;
}
