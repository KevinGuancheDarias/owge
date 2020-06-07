import { MissionReportJson } from './mission-report-json.type';
import { Unit } from './unit.type';

/**
 * Represents a explore mission
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
export interface MissionReportExploreJson extends MissionReportJson {
    unitsInPlanet?: Unit[];
}
