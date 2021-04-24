import { InterceptionInfo } from './interception-info.type';
import { MissionReportJson } from './mission-report-json.type';

export interface MissionReportInterceptedJson extends MissionReportJson {
    interceptionInfo: InterceptionInfo[];
}
