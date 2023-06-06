
import { Observable } from 'rxjs';
import { AbstractConfigurationCrudService } from '../../services/abstract-configuration-crud.service';
import { UniverseGameService } from '../../services/universe-game.service';


/**
 * Adds the delete() method
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 * @template T
 * @template K
 */
export class WithDeleteCrudMixin<T, K> extends AbstractConfigurationCrudService<T, K> {

    protected _universeGameService: UniverseGameService;

    /**
     * Deletes an entity
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param  id
     * @returns The list of all entities with the one affected removed
     */
    public delete(id: K): Observable<T[]> {
        const path = this._findOneEntityPath(id);
        this._universeGameService.requestWithAutorizationToContext(this._getContextPathPrefix(), 'delete', path).subscribe(() => {
            if (this._data && this._subject) {
                this._data = this._data.filter(current => <any>current[this._getIdKey()] !== id);
                this._syncSubject();
            }
        });
        return this._subject
            ? this._subject.asObservable()
            : null;
    }
}
