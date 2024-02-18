import { Subject, ReplaySubject } from 'rxjs';
import {CommonEntity} from '@owge/types/core';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export class SpeedImpactGroupStore {
    public readonly unlockedIds: Subject<number[]> = new ReplaySubject(1);
    public readonly content: Subject<CommonEntity[]> = new ReplaySubject(1);
}
