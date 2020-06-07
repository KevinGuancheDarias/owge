import { MissionReportJson } from './mission-report-json.type';

export interface MissionReportErrorJson extends MissionReportJson {
    errorText: string;
}
