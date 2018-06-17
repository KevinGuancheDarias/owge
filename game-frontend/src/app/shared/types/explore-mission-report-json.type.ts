import { MissionReportJson } from './mission-report-json.type';
import { UnitPojo } from '../../shared-pojo/unit.pojo';


/**
 * Represents a explore mission
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface ExploreMissionReportJson
 * @extends {MissionReportJson}
 */
export interface ExploreMissionReportJson extends MissionReportJson {
    unitsInPlanet?: UnitPojo[];
}
