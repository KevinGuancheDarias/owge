import { MissionSupport } from './mission-support.type';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface TypeWithMissionLimitation {
    canConquest: MissionSupport;
    canCounterattack: MissionSupport;
    canDeploy: MissionSupport;
    canEstablishBase: MissionSupport;
    canAttack: MissionSupport;
    canExplore: MissionSupport;
    canGather: MissionSupport;
}
