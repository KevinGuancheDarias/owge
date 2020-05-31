import { MissionReportJson } from './mission-report-json.type';
import { AliveDeathObtainedUnit } from '../../../../game-frontend/src/app/shared/pojos/alive-death-obtained-unit.pojo';

export interface MissionReportAttackJson extends MissionReportJson {
    attackInformation: {
        userInfo: {
            id: number;
            username: string;
        };
        earnedPoints: number;
        units: AliveDeathObtainedUnit[]
    }[];
}
