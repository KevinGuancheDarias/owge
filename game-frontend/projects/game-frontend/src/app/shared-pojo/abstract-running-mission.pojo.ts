import { MissionType } from '../shared/types/mission.type';

export abstract class AbstractRunningMissionPojo {
    public missionId;
    public requiredPrimary: number;
    public requiredSecondary: number;
    public pendingMillis: number;
    public type: MissionType;

    /**
     * Using the pending millis to compute the locally valid termination date
     *
     * @since 0.8.1
     */
    public browserComputedTerminationDate?: Date;

    /**
     *
     * @since 0.8.0
     */
    public missionsCount?: number;
}
