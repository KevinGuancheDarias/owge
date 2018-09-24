import { ExploreMissionReportJson } from './explore-mission-report-json.type';
import { AttackMissionReportJson } from './attack-mission-report-json.type';
import { ErrorMissionReportJson } from './error-mission-report-json.type';

/**
 * Represents any type of mission, "Extends all mission types"
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface AnyMissionReportJson
 * @extends {ExploreMissionReportJson}
 * @extends {AttackMissionReportJson}
 */
export interface AnyMissionReportJson extends ExploreMissionReportJson, AttackMissionReportJson, ErrorMissionReportJson {

}
