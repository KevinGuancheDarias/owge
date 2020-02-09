import { MissionReportJson } from './mission-report-json.type';


/**
 * Represens a establish base mission
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 * @export
 * @interface EstablishBaseMissionReportJson
 * @extends {MissionReportJson}
 */
export interface EstablishBaseMissionReportJson extends MissionReportJson {
    establishBaseStatus: boolean;
    establishBaseStatusStr: string;
}
