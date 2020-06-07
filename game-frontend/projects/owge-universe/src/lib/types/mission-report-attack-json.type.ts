import { MissionReportJson } from './mission-report-json.type';
import { AliveDeathObtainedUnit } from './alive-death-obtained-unit.type';

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
