
/**
 * Represents an improvement Unit Type
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface ImprovementUnitType {
    id?: number;
    type: 'ATTACK' | 'DEFENSE' | 'SHIELD';
    unitTypeId: number;
    unitTypeName?: string;
    value: number;
}
