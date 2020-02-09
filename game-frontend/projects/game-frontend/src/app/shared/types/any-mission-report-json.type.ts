import { ExploreMissionReportJson } from './explore-mission-report-json.type';
import { AttackMissionReportJson } from './attack-mission-report-json.type';
import { GatherMissionReportJson } from './gather-mission-report-json.type';
import { ErrorMissionReportJson } from './error-mission-report-json.type';
import { EstablishBaseMissionReportJson } from './establish-base-mission-report.type';

/**
 * Represents any type of mission, "Extends all mission types"
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface AnyMissionReportJson
 * @extends {ExploreMissionReportJson}
 * @extends {AttackMissionReportJson}
 */
export interface AnyMissionReportJson
    extends ExploreMissionReportJson, GatherMissionReportJson, AttackMissionReportJson, ErrorMissionReportJson,
    EstablishBaseMissionReportJson {

}
