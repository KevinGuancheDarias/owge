import { CriticalAttack, SpeedImpactGroup } from '../core';
import { InterceptableSpeedGroup } from './interceptable-speed-group.type';

/**
 * Represents an unit
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface Unit<R = any> {
    id: number;
    order: number;
    name: string;
    image?: number;
    imageUrl?: string;
    hasToDisplayInRequirements?: boolean;
    points?: number;
    description?: string;
    time?: number;
    primaryResource?: number;
    secondaryResource?: number;
    improvement?: any;
    energy?: number;
    attack?: number;
    health?: number;
    shield?: number;
    charge?: number;
    isUnique?: boolean;
    canFastExplore?: boolean;
    speed: number;
    typeId: number;
    typeName?: string;
    requirements?: R;
    speedImpactGroup: SpeedImpactGroup;
    criticalAttack: CriticalAttack;

    /**
     * @since 0.10.0
     */
    bypassShield: boolean;

    /**
     * @since 0.10.0
     */
    isInvisible: boolean;

    /**
     * @since 0.10.0
     */
    interceptableSpeedGroups: Partial<InterceptableSpeedGroup>[];

    storedWeight: number;
    storageCapacity: number;
}
