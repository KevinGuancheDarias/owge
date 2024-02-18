import { UnitType } from '@owge/types/core';
import { ReplaySubject, Subject } from 'rxjs';


/**
 * Contains the information related with the Unit Types
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export class UnitTypeStore {
    public userValues: Subject<UnitType[]> = new ReplaySubject(1);
}
