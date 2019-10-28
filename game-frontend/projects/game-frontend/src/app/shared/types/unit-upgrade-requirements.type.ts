import { UnitPojo } from '../../shared-pojo/unit.pojo';
import { Upgrade } from '../../shared-pojo/upgrade.pojo';

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
