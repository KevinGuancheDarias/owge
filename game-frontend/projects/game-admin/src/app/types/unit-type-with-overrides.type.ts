import { UnitType } from '@owge/core';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.10.0
 * @export
 */
export interface UnitTypeWithOverrides extends UnitType {
    overrideId?: number;
    overrideMaxCount?: number;
    isOverride?: boolean;
}
