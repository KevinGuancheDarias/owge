import { Upgrade, Unit } from '@owge/universe';

/**
 * Represents the pair unit, and upgrades required to build that unit
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface UnitUpgradeRequirements
 */
export interface UnitUpgradeRequirements {
    unit: Unit;
    requirements: {
        upgrade: Upgrade,
        level: number,
        reached: boolean
    }[];
}
