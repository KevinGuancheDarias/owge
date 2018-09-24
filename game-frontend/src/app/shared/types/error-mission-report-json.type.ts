import { MissionReportJson } from './mission-report-json.type';

export interface ErrorMissionReportJson extends MissionReportJson {
    errorText: string;
}
