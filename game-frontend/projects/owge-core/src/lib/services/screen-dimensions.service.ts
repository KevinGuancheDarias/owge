import { Injectable, Type } from '@angular/core';
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';

import { LoggerHelper } from '../helpers/logger.helper';

type targetWindowProp = 'innerWidth' | 'innerHeight';

class SubjectAndExpectaction {
    public subject: Subject<boolean>;
    public expectedPx: number;
    public timeoutId: number;
    public identifier: string;
    public targetProp: targetWindowProp;

    public constructor(subject: Subject<boolean>, expectedPx: number, identifier: string, targetProp: targetWindowProp) {
        this.subject = subject;
        this.expectedPx = expectedPx;
        this.identifier = identifier;
        this.targetProp = targetProp;
        this.timeoutId = 0;
    }

}

/**
 * Has methods to interact with the screen dimensions <br>
 * <b>Hint: </b> It's the equivalent to CSS media queries
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 * @export
 */
@Injectable()
export class ScreenDimensionsService {
    protected _subjects: Map<string, SubjectAndExpectaction> = new Map();

    private _log: LoggerHelper = new LoggerHelper(this.constructor.name);

    public constructor() {
        window.addEventListener('resize', () => {
            this._subjects.forEach(current => {
                this._resizeHandler(current);
            });
        });
    }

    /**
     * Generates an unique identifier (that doesn't exists in the map)
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     * @param clazz
     * @returns
     */
    public generateIdentifier(clazz: Object): string {
        const doGen = () => `${clazz.constructor.name}_${new Date().getTime()}_${Math.random().toPrecision(8)}`;
        let identifier = doGen();
        while (this._subjects.get(identifier)) {
            identifier = doGen();
        }
        return identifier;
    }

    /**
     * Reacts to screen resize and notifies true or false depending on the screen width
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     * @param expectedPx
     * @param identifier Used to be able to remove the handler from the system
     *  (for example on a ngDestruct of a component that uses this listener)
     * @returns
     */
    public hasMinWidth(expectedPx: number, identifier: string): Observable<boolean> {
        return this._hasMinProp('innerWidth', expectedPx, identifier);
    }


    /**
     * Reacts to screen resize and notifies true or false depending on the screen height
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     * @param expectedPx
     * @param identifier Used to be able to remove the handler from the system
     *  (for example on a ngDestruct of a component that uses this listener)
     * @returns
     */
    public hasMinHeight(expectedPx: number, identifier: string): Observable<boolean> {
        return this._hasMinProp('innerHeight', expectedPx, identifier);
    }

    /**
     * Removes a handler from the service
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     * @param identifier
     */
    public removeHandler(identifier: string): void {
        this._log.debug(`Removing handler ${identifier}`);
        const subjectAndExpectation: SubjectAndExpectaction = this._subjects.get(identifier);
        if (subjectAndExpectation) {
            subjectAndExpectation.subject.unsubscribe();
            this._subjects.delete(identifier);
        } else {
            this._log.warn(`No subject with identifier ${identifier}`);
        }
    }

    private _hasMinProp(prop: targetWindowProp, expectedPx: number, identifier: string): Observable<boolean> {
        this._log.debug(`Adding handler ${identifier} with expectation ${expectedPx}`);
        const subjectAndExpectation: SubjectAndExpectaction = new SubjectAndExpectaction(
            new BehaviorSubject(this._isGreaterThanExpectedWidth(prop, expectedPx)), expectedPx, identifier, prop
        );
        this._subjects.set(identifier, subjectAndExpectation);
        return subjectAndExpectation.subject.asObservable().pipe(distinctUntilChanged());
    }

    private _resizeHandler(subjectAndExpectation: SubjectAndExpectaction): void {
        if (subjectAndExpectation.timeoutId) {
            clearTimeout(subjectAndExpectation.timeoutId);
            subjectAndExpectation.timeoutId = 0;
        }
        subjectAndExpectation.timeoutId = setTimeout(() => {
            if (this._subjects.get(subjectAndExpectation.identifier)) {
                subjectAndExpectation.subject.next(
                    this._isGreaterThanExpectedWidth(subjectAndExpectation.targetProp, subjectAndExpectation.expectedPx)
                );
            }
            subjectAndExpectation.timeoutId = 0;
        });
    }

    private _isGreaterThanExpectedWidth(targetProp: targetWindowProp, expectedPx: number): boolean {
        return window[targetProp] >= expectedPx;
    }
}
