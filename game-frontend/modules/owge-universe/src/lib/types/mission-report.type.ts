import { MissionReportJson } from './mission-report-json.type';
import { MissionReportAnyJson } from './mission-report-any-json.type';

/**
 * Represents a common mission report
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @template T extends MissionReportJson, type of mission
 */
export interface MissionReport<T extends MissionReportJson = MissionReportAnyJson> {
    id: number;
    missionId?: number;
    missionDate?: Date;
    reportDate: Date;
    userReadDate: Date;
    isEnemy: boolean;
    parsedJson: T;
}
