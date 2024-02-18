import { validContext, ProgrammingError, LoggerHelper } from '@owge/core';

import { CrudServiceAuthControl } from '@owge/types/universe';
import { Subject } from 'rxjs';

/**
 * Extend this class to force a present implementation of the crud configuration <br>
 * <b>NOTICE:</b> The reason why the methods are not abstract, it's because mixin wouldn't work
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export abstract class AbstractConfigurationCrudService<T, K> {
    protected _subject: Subject<T[]>;
    protected _data: T[];
    protected _log: LoggerHelper = new LoggerHelper(this.constructor.name);

    protected _getEntity(): string {
        throw new ProgrammingError('Must implement the getEntity() method in the invoker Crud');
    }
    protected _getContextPathPrefix(): validContext {
        throw new ProgrammingError('Must implement the getEntity() method in the invoker Crud');
    }
    protected _getAuthConfiguration(): CrudServiceAuthControl {
        throw new ProgrammingError('Must implement the getEntity() method in the invoker Crud');
    }

    protected _syncSubject(): void {
        this._log.debug('Updating subject', <any>this._data);
        this._subject.next(this._data);
    }

    /**
     * Finds the id key for the given entity <br>
     * If not overriden will default to 'id'
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @returns
     */
    protected _getIdKey(): keyof T {
        return <any>'id';
    }

    /**
     * Returns the HTTP URI Path to fetch, save, delete a single existing entity
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param id
     * @returns
     */
    protected _findOneEntityPath(id: K): string {
        return `${this._getEntity()}/${id}`;
    }

}
