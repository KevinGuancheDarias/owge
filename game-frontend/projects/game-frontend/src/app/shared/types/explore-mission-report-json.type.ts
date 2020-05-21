import { MissionReportJson } from './mission-report-json.type';
import { Unit } from '@owge/universe';


/**
 * Represents a explore mission
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface ExploreMissionReportJson
 * @extends {MissionReportJson}
 */
export interface ExploreMissionReportJson extends MissionReportJson {
    unitsInPlanet?: Unit[];
}
