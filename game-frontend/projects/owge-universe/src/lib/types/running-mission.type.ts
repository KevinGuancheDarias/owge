import { MissionType } from '@owge/core';

/**
 *
 * @deprecated As of 0.9.0 this is in @owge/universe
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface RunningMission {
    missionId;
    requiredPrimary: number;
    requiredSecondary: number;
    pendingMillis: number;
    type: MissionType;

    /**
     * Using the pending millis to compute the locally valid termination date
     *
     * @since 0.8.1
     */
    browserComputedTerminationDate?: Date;

    /**
     *
     * @since 0.8.0
     */
    missionsCount?: number;
}
