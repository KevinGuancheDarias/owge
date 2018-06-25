import { MissionReportJson } from './mission-report-json.type';
import { AnyMissionReportJson } from './any-mission-report-json.type';

/**
 * Represents a common mission report
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @template T extends MissionReportJson, type of mission
 * @interface MissionReport
 */
export interface MissionReport<T extends MissionReportJson = AnyMissionReportJson> {
    missionId?: number;
    missionDate?: Date;
    reportDate: Date;
    id: number;
    parsedJson: T;
}
