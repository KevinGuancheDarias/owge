import { ReplaySubject, Subject } from 'rxjs';
import { SystemMessage } from '../types/system-message.type';

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
