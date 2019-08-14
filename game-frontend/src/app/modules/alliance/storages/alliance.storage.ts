import { Injectable } from '@angular/core';
import { ReplaySubject } from 'rxjs';

import { Alliance } from '../types/alliance.type';

/**
 * Contains storages associated with the Alliance
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @class AllianceStorage
 */
@Injectable()
export class AllianceStorage {


    /**
     * Holds the current user alliance
     *
     * @since 0.7.0
     * @type {ReplaySubject<Alliance>}
     * @memberof AllianceStorage
     */
    public readonly userAlliance: ReplaySubject<Alliance> = new ReplaySubject(1);
}
