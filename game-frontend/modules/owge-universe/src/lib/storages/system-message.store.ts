import { ReplaySubject, Subject } from 'rxjs';
import { SystemMessage } from '@owge/types/universe';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.16
 * @export
 */
export class SystemMessageStore {

    /**
     *
     * @since 0.9.16
     */
    public readonly messages: Subject<SystemMessage[]> = new ReplaySubject(1);
}
