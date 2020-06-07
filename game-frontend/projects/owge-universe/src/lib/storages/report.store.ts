import { Subject, ReplaySubject } from 'rxjs';
import { MissionReport } from '../types/mission-report.type';

/**
 * Represents the report store
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export class ReportStore {
    public readonly reports: Subject<MissionReport[]> = new ReplaySubject(1);
    public readonly userUnread: Subject<number> = new ReplaySubject(1);
    public readonly enemyUnread: Subject<number> = new ReplaySubject(1);
}
