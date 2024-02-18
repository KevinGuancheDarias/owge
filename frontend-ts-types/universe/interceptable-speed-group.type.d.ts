import { SpeedImpactGroup } from '../core';
import { Unit } from './unit.type';

/**
 * Represents an unit that can intercept the specified speedImpactGroup
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.10.0
 * @export
 */
export interface InterceptableSpeedGroup {
    id: number;
    unit: Unit;
    speedImpactGroup: SpeedImpactGroup;
}
