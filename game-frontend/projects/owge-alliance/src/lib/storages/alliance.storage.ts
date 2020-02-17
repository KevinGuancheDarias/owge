import { Injectable } from '@angular/core';
import { ReplaySubject } from 'rxjs';

import { Alliance } from '../types/alliance.type';
import { AllianceJoinRequest } from '../types/alliance-join-request.type';

/**
 * Contains storages associated with the Alliance
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
@Injectable()
export class AllianceStorage {

    /**
     * Holds the current user alliance
     *
     * @since 0.7.0
     */
    public readonly userAlliance: ReplaySubject<Alliance> = new ReplaySubject(1);

    /**
     * Holds the current user join requests sent
     *
     * @since 0.8.1
     */
    public readonly userJoinRequests: ReplaySubject<AllianceJoinRequest[]> = new ReplaySubject(1);
}
