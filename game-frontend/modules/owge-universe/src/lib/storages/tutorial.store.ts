import { TutorialSectionEntry } from '@owge/types/universe';
import { ReplaySubject, Subject } from 'rxjs';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export class TutorialStore {
    public readonly entries: Subject<TutorialSectionEntry[]> = new ReplaySubject(1);
    public readonly visitedEntries: Subject<number[]> = new ReplaySubject(1);
}
