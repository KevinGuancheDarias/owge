import { MissionReportJson } from './mission-report-json.type';
import { UserPojo } from '../../shared-pojo/user.pojo';
import { ObtainedUnit } from '../../shared-pojo/obtained-unit.pojo';
import { AliveDeathObtainedUnit } from '../pojos/alive-death-obtained-unit.pojo';

export interface AttackMissionReportJson extends MissionReportJson {
    attackInformation: {
        userInfo: {
            id: number;
            username: string;
        };
        earnedPoints: number;
        units: AliveDeathObtainedUnit[]
    }[];
}
