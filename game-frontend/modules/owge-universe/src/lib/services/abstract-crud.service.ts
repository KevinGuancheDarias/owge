import { UniverseGameService } from './universe-game.service';
import { Observable, Subject } from 'rxjs';

import { ProgrammingError, LoggerHelper } from '@owge/core';

import { CrudConfig } from '@owge/types/universe';
import { take, map } from 'rxjs/operators';
import { WithReadCrudMixin } from '../mixins/services/with-read-crud.mixin';
import { WithDeleteCrudMixin } from '../mixins/services/with-delete-crud.mixin';
import { Mixin } from 'ts-mixer';
import { StoreAwareService } from '../interfaces/store-aware-service.interface';

/**
 * Has default methods for the target entities <br>
 * Remember that you can also override <i>_getIdKey()</i> <br>
 *
 * <b>Notice:</b> The class doesn't have the abstract keyword, because @mix wouldn' work,
 * but we "protect " it by convertir the constructor to protected
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
export class AbstractCrudService<T, K = number> extends WithReadCrudMixin<T, K> implements StoreAwareService {
    protected _log: LoggerHelper = new LoggerHelper(this.constructor.name);
    protected _subject: Subject<T[]>;
    protected _data: T[];

    /**
     * Creates an instance of AbstractCrudServiceInner.
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param _universeGameService
     */
    public constructor(protected _universeGameService: UniverseGameService) {
        super();
        if (!_universeGameService) {
            throw new ProgrammingError('When extending AbstractCrudService, it is required to pass UniverseGameService to the constructor');
        }
    }

    /**
     * Saves an entity to the backend
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param entity
     * @returns Saved entity with id
     */
    public saveNew(entity: T): Observable<T> {
        const path = `${this._getEntity()}`;
        return this._universeGameService.requestWithAutorizationToContext(
            this._getContextPathPrefix(),
            'post',
            path,
            entity
        ).pipe(
            map(saved => {
                if (this._data) {
                    this._data.push(saved);
                    this._syncSubject();
                }
                return saved;
            }),
            take(1)
        );
    }

    /**
     * Saves an existing entity, or if backend supports it, creates by PUT (trusting frontend provided id)
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param  entity
     * @returns
     */
    public saveExistingOrPut(entity: T): Observable<T> {
        if (!entity[this._getIdKey()]) {
            throw new ProgrammingError('saveExistingOrPut requires the id to be truthy');
        }
        const path = this._findOneEntityPath(<any>entity[this._getIdKey()]);
        return this._universeGameService.requestWithAutorizationToContext(
            this._getContextPathPrefix(),
            'put',
            path,
            entity
        ).pipe(
            map(saved => {
                if (this._data) {
                    this._data = this._data.map(current => current[this._getIdKey()] === saved[this._getIdKey()] ? saved : current);
                    this._syncSubject();
                    this._log.debug('Emmited new subject value for saving');
                }
                return saved;
            }),
            take(1)
        );
    }

    /**
     * Finds the current crud config
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public getCrudConfig(): CrudConfig {
        return {
            entityPath: this._getEntity(),
            contextPath: this._getContextPathPrefix(),
            authConfiguration: this._getAuthConfiguration(),
            findOneEntityPath: (id) => this._findOneEntityPath(id)
        };
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public getChangeObservable(): Observable<any> {
        return this._subject.asObservable();
    }

}
export interface AbstractCrudService<T, K = number> extends WithReadCrudMixin<T, K>, WithDeleteCrudMixin<T, K> { }
(<any>AbstractCrudService) = Mixin(WithDeleteCrudMixin, <any>AbstractCrudService);
