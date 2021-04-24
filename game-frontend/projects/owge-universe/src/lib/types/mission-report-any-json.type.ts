import { MissionReportExploreJson } from './mission-report-explore-json.type';
import { MissionReportAttackJson } from './mission-report-attack-json.type';
import { MissionReportGatherJson } from './mission-report-gather-json.type';
import { MissionReportErrorJson } from './mission-report-error-json.type';
import { MissionReportEstablishBaseJson } from './mission-report-establish-base-json.type';
import { MissionReportConquestJson } from './mission-report-conquest-json.type';
import { MissionReportInterceptedJson } from './mission-report-intercepted-json.type';

/**
 * Represents any type of mission, "Extends all mission types"
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
export interface MissionReportAnyJson
    extends MissionReportExploreJson, MissionReportGatherJson, MissionReportAttackJson, MissionReportErrorJson,
    MissionReportEstablishBaseJson, MissionReportConquestJson, MissionReportInterceptedJson {

}
