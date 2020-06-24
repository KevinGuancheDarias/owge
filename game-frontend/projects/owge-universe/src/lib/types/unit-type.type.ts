import { MissionSupport } from './mission-support.type';
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
    computedMaxCount?: number;
    userBuilt: number;
    speedImpactGroup: SpeedImpactGroup;
}
