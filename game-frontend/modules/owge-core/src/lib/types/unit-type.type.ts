import { AttackRule } from './attack-rule.type';
import { CriticalAttack } from './critical-attack.type';
import { SpeedImpactGroup } from './speed-impact-group.type';
import { TypeWithMissionLimitation } from './type-with-mission-limitation.type';

/**
 * Represents a UnitType as sent by backend
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
export interface UnitType extends TypeWithMissionLimitation {
    id: number;
    name: string;
    image: number;
    imageUrl: string;
    maxCount?: number;
    parent: UnitType;
    shareMaxCount: UnitType;
    computedMaxCount?: number;
    userBuilt: number;
    speedImpactGroup: SpeedImpactGroup;
    hasToInheritImprovements: boolean;
    attackRule: AttackRule;
    criticalAttack: CriticalAttack;

    /**
     * If the unit type is used by any unit
     *
     * @since 0.9.20
     */
    used: boolean;
}
