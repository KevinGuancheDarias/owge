import { MissionType } from '../shared/types/mission.type';

export abstract class AbstractRunningMissionPojo {
    public missionId;
    public requiredPrimary: number;
    public requiredSecondary: number;
    public terminationDate: Date;
    public type: MissionType;

    /**
     *
     * @since 0.8.0
     */
    public missionsCount?: number;
}
