import { ProgrammingError } from './../error/programming.error';
import { Observable } from 'rxjs/Observable';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Router } from '@angular/router';
import { ServiceLocator } from './../app/service-locator/service-locator';
import { TestBed, async, TestModuleMetadata } from '@angular/core/testing';
import { Injector, Type } from '@angular/core';

type StatePrefix = 'f' | 'x' | '';
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

    protected _targetClass: Type<T>;

    /**
     * Apply the settings to TestBed and compile components
     *
     * @param {TestModuleMetadata} config declarations, providers, etc
     * @returns {this}
     * @memberOf AbstractCommonTestHelper
     * @author Kevin Guanche Darias
     */
    public configureTestingModule(config: TestModuleMetadata): this {
        beforeEach(async(() => {
            TestBed.configureTestingModule(config).compileComponents();
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
     * @param {*} [newValue] (Optionally) specify new returnVal
     * @memberOf TestUtil
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public resetSpyCalls(spyMethod: any, newValue?: any): void {
        spyMethod.calls.reset();
        if (typeof newValue !== 'undefined') {
            spyMethod.and.returnValue(newValue);
        }
    }
}
