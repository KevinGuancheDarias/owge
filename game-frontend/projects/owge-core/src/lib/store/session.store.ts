import { Injectable } from '@angular/core';
import {  ReplaySubject, Observable, Subject } from 'rxjs';

import { LoggerHelper } from '../helpers/logger.helper';
import { ProgrammingError } from '../errors/programming.error';

/**
 * Stores the created subject, and the original observable associated with it
 */
interface SubjectAndSource {
    subject: ReplaySubject<any>;
    source?: Subject<any>;
}

/**
 * This store unifies all session related stores. It itself doesn't implement anything <br>
 * The observables here may exists in other particular stores such as UserStore, PlanetStore, UniverseStore etc
 * So... why is this required?
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable({
    providedIn: 'root'
})
export class SessionStore {
    private _replaySubjectsStore: { [key: string]: SubjectAndSource } = {};
    private _log: LoggerHelper = new LoggerHelper(this.constructor.name);

    /**
     * Returns the subject (Note, source may not be attached, and may never be.. if the key is wrong)
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @template T
     * @param  key
     * @returns
     */
    public get<T>(key: string): Observable<T> {
        return this._getSubjectAndSource(key).subject.asObservable();
    }

    /**
     * Applyes a next to the subject <br>
     * <b>NOTICE:</b> While possible should NOT be done, as breaks SOLID
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param key
     * @param value
     */
    public next(key: string, value: any): void {
        this._log.warn(
            `Invoking next() to observable with key ${key} and new value ${value}`,
            `Note, next() while supported should not be used from SessionStore, this usually means something needs a refactor, or rethink`
        );
        if (!this._existsAndHasSource(key)) {
            throw new ProgrammingError(`Can't invoke next, when observable with key ${key} has no source subject`);
        }
        this._getSubjectAndSource(key).source.next(value);
    }

    /**
     * Adds a new subject to the internal store
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param  key
     * @param [observable]
     * @param [length=1]
     * @memberof SessionStore
     */
    public addSubject( key: string, observable?: Subject<any>, length = 1 ): void {
        if (!this._replaySubjectsStore[key]) {
            this._replaySubjectsStore[key] = {
                subject: new ReplaySubject(length),
                source: observable
            };
        } else if (!this._replaySubjectsStore[key].source && observable) {
            this._replaySubjectsStore[key].source = observable;
        }
        observable.subscribe(value => this._replaySubjectsStore[key].subject.next(value));
    }

    private _existsAndHasSource(key: string): boolean {
        return !!(this._replaySubjectsStore[key] && this._replaySubjectsStore[key].source);
    }

    private _getSubjectAndSource(key: string): SubjectAndSource {
        if (!this._replaySubjectsStore[key]) {
            this._log.debug(`Tryed to get subject ${key}, that was not initialized`);
            this.addSubject(key);
        }
        return this._replaySubjectsStore[key];
    }
}
