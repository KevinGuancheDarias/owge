import { Observable } from 'rxjs';

/**
 * Services that have a store and use filters, should implements this interface, in order to the filters to refresh automatically
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface StoreAwareService {
    getChangeObservable(): Observable<any>;
}
