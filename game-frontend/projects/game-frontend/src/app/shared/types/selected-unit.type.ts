import { Unit } from '@owge/universe';

/**
 * Represents the selection of the unit
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface SelectedUnit
 */
export interface SelectedUnit {
    /**
     * Id of the unit (UnitPojo)
     *
     * @type {number}
     * @memberof SelectedUnit
     */
    id?: number;

    unit?: Unit;

    /**
     * Selected count
     *
     * @type {number}
     * @memberof SelectedUnit
     */
    count: number;

    expirationId: number;
}
