import { MissionReportJson } from './mission-report-json.type';


/**
 * Represents a Gather report JSON
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface GatherMissionReportJson
 * @extends {MissionReportJson}
 */
export interface GatherMissionReportJson extends MissionReportJson {
    gatheredPrimary: number;
    gatheredSecondary: number;
}
