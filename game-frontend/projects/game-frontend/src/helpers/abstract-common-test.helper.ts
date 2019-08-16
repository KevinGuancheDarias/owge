import { ProgrammingError } from './../error/programming.error';
import { Observable, BehaviorSubject } from 'rxjs';
import { Router } from '@angular/router';
import { ServiceLocator } from './../app/service-locator/service-locator';
import { TestBed, async, TestModuleMetadata } from '@angular/core/testing';
import { Injector, Type } from '@angular/core';

export type StatePrefix = 'f' | 'x' | '';
export abstract class AbstractCommonTestHelper<T> {
    /**
     * Returns the last observable defined by mockReturnedObservable <br>
     * This method is ideal when concatenating multiple test
     * @readonly
     * @type {Observable<any>}
     * @memberOf AbstractCommonTestHelper
     */
    public get lastDefinedObservable(): Observable<any> {
        return this._lastDefinedObservable;
    }
    protected _lastDefinedObservable: Observable<any>;

    protected _targetClass: Type<T> | string;

    protected _usedConfig: TestModuleMetadata;

    public constructor(targetClass: Type<T> | string) {
        this._targetClass = targetClass;
    }

    /**
     * Apply the settings to TestBed and compile components
     *
     * @param {TestModuleMetadata} [config] declarations, providers, etc, if not <b>defined will use the passed in the constructor</b>
     * @returns {this}
     * @memberOf AbstractCommonTestHelper
     * @author Kevin Guanche Darias
     */
    public configureTestingModule(config?: TestModuleMetadata): this {
        beforeEach(async(() => {
            TestBed.configureTestingModule(config || this._usedConfig).compileComponents();
        }));
        return this;
    }

    /**
     * Sets the ServiceLocator injector
     *
     * @returns {this}
     * @memberOf AbstractCommonTestHelper
     * @author Kevin Guanche Darias
     */
    public configureServiceLocator(): this {
        beforeEach(() => {
            ServiceLocator.injector = TestBed.get(Injector);
        });
        return this;
    }

    /**
     * Will run the action before each test <br>
     * <b>IMPORTANT:</b> If code is async, surprises may occur, use beforeEachAsync() instead
     *
     * @param {(Function) => void} action Action to execute before each test
     * @returns {thos}
     *
     * @memberOf CommonComponentTestHelper
     * @author Kevin Guanche Darias
     */
    public beforeEach(action: (Function) => void): this {
        if (typeof action === 'function') {
            beforeEach(action);
        }
        return this;
    }

    /**
     * Uses a mock router instead of the real <br>
     * It's commonly useless to test the router itself <br>
     * Just test that the router.navigate() method has been used <br>
     * <b>NOTICE:</b> This method makes router instance spying navigate()
     *
     * @memberOf CommonComponentTestHelper
     * @author Kevin Guanche Darias
     */
    public withMockRouterBeforeEach(): this {
        this.beforeEach(() => {
            const routerInstance: Router = TestBed.get(Router);
            spyOn(routerInstance, 'navigate').and.returnValue(true);
        });
        return this;
    }

    /**
     * Mocks subscribe response from class <br>
     * NOTICE: Call from inside an it() or beforeEach() block, or unexpected behavior may occur! <br>
     * Notice: Use inside a block with the async() function or unexpected behavior WILL PROBABLY occur
     *
     * @deprecated Unrequired function, use spyOn(object, methodName).and.returnValue(Observable.of(retVal)) instead
     * @param {FunctionConstructor} object object to which the method should be
     * @param {string} methodName name of the method in the object
     * @param {*} retVal Value that the observable should return
     * @memberOf AbstractCommonTestHelper
     * @author Kevin Guanche Darias
     */
    public mockReturnedObservable(object: FunctionConstructor, methodName: string, retVal: any): this {
        if (!object[methodName]) {
            throw new ProgrammingError('No method called ' + methodName + ' exists in ' + object.constructor.name);
        }
        const observable: BehaviorSubject<any> = new BehaviorSubject(null);
        spyOn(object, <any>methodName).and.returnValue(observable);
        observable.next(retVal);
        return this;
    }

    /**
     * Runs a jasmine test (it) that haves async content
     *
     * @param {string} testText text of the test
     * @param {(Function) => void} action action code to run
     * @param {StatePrefix} state If it's prioritary of skipped ('f' or 'x' or '')
     * @returns {this}
     * @author Kevin Guanche Darias
     * @memberOf AbstractCommonTestHelper
     */
    public testAsync(testText: string, action: (Function) => void, state: StatePrefix = ''): this {
        let itFunction: Function;
        if (state === 'f') {
            itFunction = fit;
        } else if (state === 'x') {
            itFunction = xit;
        } else {
            itFunction = it;
        }
        itFunction(testText, async(action));
        return this;
    }

    /**
     * This test is prioritary (fit)
     *
     * @param {string} textText
     * @param {(Function) => void} action
     * @returns {this}
     * @author Kevin Guanche Darias
     * @memberOf AbstractCommonTestHelper
     */
    public ftestAsync(textText: string, action: (Function) => void): this {
        this.testAsync(textText, action, 'f');
        return this;
    }

    /**
     * Skips this test! (xit)
     *
     * @param {string} textText
     * @param {(Function) => void} action
     * @returns {this}
     * @memberOf AbstractCommonTestHelper
     * @author Kevin Guanche Darias
     */
    public xtestAsync(textText: string, action: (Function) => void): this {
        this.testAsync(textText, action, 'x');
        return this;
    }


    /**
     * Returns an instance from the Angular context <br>
     * <b>IMPORTANT:</b> ServiceLocator must have been initialized, before using this
     *
     * @param {any} beanType objectType
     * @returns {any} Instance of ObjectType
     *
     * @memberOf AbstractCommonTestHelper
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public getBean<B>(beanType: any): any {
        return ServiceLocator.injector.get(beanType);
    }

    /**
     * Resets the spy, just as if it was not used, before in this it()
     *
     * @param {*} spyMethod spy method to reset
     * @param {*} [newValue] (Optionally) specify new returnVal, if a function, will call as passed "fake function"
     * @memberOf TestUtil
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public resetSpyCalls(spyMethod: any, newValue?: any): void {
        spyMethod.calls.reset();
        if (typeof newValue === 'function') {
            spyMethod.and.callFake(newValue);
        } else if (typeof newValue !== 'undefined') {
            spyMethod.and.returnValue(newValue);
        }
    }

    /**
     * Returns as a promise
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {any} [retVal] Value to return
     * @returns Promise with <i>retVal</i> as value
     * @memberof AbstractCommonTestHelper
     */
    public async promiseReturn(retVal?: any): Promise<any> {
        return retVal;
    }

    /**
     * "Jasmine expects" the input to be an array
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {any[]} targetData array to check if it's an array
     * @returns {boolean} true if input is an array
     * @memberof AbstractCommonTestHelper
     */
    public expectArray(targetData: any[]): boolean {
        expect(targetData).toEqual(jasmine.any(Array), 'input is not an array');
        return targetData instanceof Array;
    }

    /**
     * Executes jasmine expects() over all objects in <i>data</i> array
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {any[]} data data to test
     * @param {Function} [targetClass] expected class, if not specified will use the helper target class
     * @throws {ProgrammingError} When targetClass is not defined, and default one is a string
     * @memberof AbstractCommonTestHelper
     */
    public expectInstancesOf(data: any[], targetClass?: Function): void {
        let finalTargetClass: Function;
        if (targetClass) {
            finalTargetClass = targetClass;
        } else if (typeof this._targetClass !== 'string') {
            finalTargetClass = this._targetClass;
        } else {
            throw new ProgrammingError(
                'Can NOT use expectInstancesOf() without targetClass argument, when the default targetClass is a string'
            );
        }

        this.expectArray(data);
        data.forEach((current, i) => {
            expect(current).toEqual(
                jasmine.any(finalTargetClass), 'object in index ' + i + ' is not an instance of ' + finalTargetClass.name
            );
            return current;
        });
    }

    /**
     * Waits for some milliseconds
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {number} [milliseconds=0] Number of milliseconds, if 0 will just resolve when all "await-ready" events are solved
     * @param {any} [value] Expected resolution value (useful when doWait is used as a value)
     * @returns {Promise<any>} Resolved value
     * @memberof AbstractCommonTestHelper
     */
    public doWait(milliseconds: number = 0, value?: any): Promise<any> {
        return new Promise(resolve => window.setTimeout(() => resolve(value), milliseconds));
    }

    /**
     * Iterates in an object, useful as it discards the object methods
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {T} target target object to iterate
     * @param {(prop: keyof T, currentValue?:any) => void} callback Action to run for each property,
     *  currentValue is the value of the prop in the object
     * @throws {ProgrammingError} If input is not an object
     * @memberof AbstractCommonTestHelper
     */
    public iterateProperties(target: T, callback: (prop: keyof T, currentValue?: any) => void) {
        if (typeof target !== 'object') {
            throw new ProgrammingError('Input target is not an object');
        }
        for (const prop in target) {
            if (target.hasOwnProperty(prop) && typeof target[prop] !== 'function') {
                callback.apply(target, [prop, target[prop]]);
            }
        }
    }

    /**
     * Spies on a method, event if that method doesn't exists <br>
     * NOTICE: Silently defines a no-action method in <i>target</i>
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {F} target
     * @param {keyof F} method
     * @returns {jasmine.Spy}
     * @memberof AbstractCommonTestHelper
     */
    public spyOn<F>(target: F, method: keyof F): jasmine.Spy {
        if (typeof target[method] !== 'undefined' && typeof target[method] !== 'function') {
            throw new ProgrammingError(`FATAL, spyOn can't be used in properties, near ${target.constructor.name}.${method}`);
        } else if (typeof target[method] === 'undefined') {
            target[method] = <any>(() => { });
        }
        return spyOn(target, method);
    }
}
