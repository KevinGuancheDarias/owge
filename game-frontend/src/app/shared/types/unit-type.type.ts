import { MissionSupport } from './mission-support.type';

/**
 * Represents a UnitType as sent by backend
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface UnitType
 */
export interface UnitType {
    id: number;
    name: string;
    image: string;
    maxCount?: number;
    computedMaxCount?: number;
    userBuilt: number;
    canConquest: MissionSupport;
    canCounterattack: MissionSupport;
    canDeploy: MissionSupport;
    canEstablishBase: MissionSupport;
    canExplore: MissionSupport;
    canGather: MissionSupport;
}
