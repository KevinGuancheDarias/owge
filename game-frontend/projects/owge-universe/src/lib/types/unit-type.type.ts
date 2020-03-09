import { MissionSupport } from './mission-support.type';

/**
 * Represents a UnitType as sent by backend
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
export interface UnitType {
    id: number;
    name: string;
    image: number;
    imageUrl: string;
    maxCount?: number;
    computedMaxCount?: number;
    userBuilt: number;
    canConquest: MissionSupport;
    canCounterattack: MissionSupport;
    canDeploy: MissionSupport;
    canEstablishBase: MissionSupport;
    canAttack: MissionSupport;
    canExplore: MissionSupport;
    canGather: MissionSupport;
}
