

/**
 * @internal
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
export type validImprovementType = 'ATTACK' | 'DEFENSE' | 'SHIELD' | 'AMOUNT';
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
    unitTypeId: number;
    unitTypeName?: string;
    value: number;
}
