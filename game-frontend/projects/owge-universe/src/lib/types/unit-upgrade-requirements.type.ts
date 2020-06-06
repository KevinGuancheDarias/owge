import { Upgrade } from './upgrade.type';
import { Unit } from './unit.type';

/**
 * Represents the pair unit, and upgrades required to build that unit
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
export interface UnitUpgradeRequirements {
    unit: Unit;
    requirements: {
        upgrade: Upgrade,
        level: number,
        reached: boolean
    }[];

    /**
     * Frontend computed property meaning, all requirements has been reached
     */
    allReached?: boolean;
}
