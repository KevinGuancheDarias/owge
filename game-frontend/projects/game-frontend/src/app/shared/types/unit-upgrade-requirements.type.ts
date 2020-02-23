import { UnitPojo } from '../../shared-pojo/unit.pojo';
import { Upgrade } from '@owge/universe';

/**
 * Represents the pair unit, and upgrades required to build that unit
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface UnitUpgradeRequirements
 */
export interface UnitUpgradeRequirements {
    unit: UnitPojo;
    requirements: {
        upgrade: Upgrade,
        level: number,
        reached: boolean
    }[];
}
