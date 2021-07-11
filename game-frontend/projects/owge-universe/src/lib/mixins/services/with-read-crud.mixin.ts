import { Observable, ReplaySubject } from 'rxjs';
import { AbstractConfigurationCrudService } from '../../services/abstract-configuration-crud.service';
import { UniverseGameService } from '../../services/universe-game.service';

/**
 * This mixing adds findAll and findOneById
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 * @template T
 * @template K
 */
export class WithReadCrudMixin<T, K> extends AbstractConfigurationCrudService<T, K> {
    protected _universeGameService: UniverseGameService;

    /**
     * Finds all entities
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public findAll(): Observable<T[]> {
        const path = `${this._getEntity()}`;
        const result: Observable<T[]> = this._getAuthConfiguration().findAll
            ? this._universeGameService.requestWithAutorizationToContext(this._getContextPathPrefix(), 'get', path)
            : this._universeGameService.getToUniverse(`${this._getContextPathPrefix()}/${path}`);
        if (!this._subject) {
            this._subject = new ReplaySubject(1);
            result.subscribe(data => {
                this._data = data;
                this._subject.next(data);
            });
        }
        return this._subject.asObservable();
    }

    /**
     * Finds one single entity with the given Id
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param  id
     * @returns
     */
    public findOneById(id: K): Observable<T> {
        const path = this._findOneEntityPath(id);
        if (this._getAuthConfiguration().findById) {
            return this._universeGameService.requestWithAutorizationToContext(this._getContextPathPrefix(), 'get', path);
        } else {
            return this._universeGameService.getToUniverse(`${this._getContextPathPrefix()}/${path}`);
        }
    }

    /**
     * Finds the unlocked entities (if the entity support this operation)
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @returns
     */
    public findUnlocked(): Observable<T[]> {
        const path = `${this._getEntity()}/findUnlocked`;
        return this._universeGameService.requestWithAutorizationToContext(this._getContextPathPrefix(), 'get', path);
    }

}
