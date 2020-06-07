import { MissionReport } from './mission-report.type';

/**
 * Represents a response
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface MissionReportResponse {
    page: number;
    userUnread: number;
    enemyUnread: number;
    reports: MissionReport[];
}
