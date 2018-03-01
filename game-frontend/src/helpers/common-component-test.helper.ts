import { By } from '@angular/platform-browser';
import { AbstractCommonTestHelper } from './abstract-common-test.helper';
import { ProgrammingError } from './../error/programming.error';
import { TestBed, TestModuleMetadata, ComponentFixture } from '@angular/core/testing';
import { DebugElement, Type } from '@angular/core';

/**
 * This helper simplifies testing configuration for a <b>component</b><br>
 * So one can assign time to the important things <br>
 * Test that the things are actually working!
 *
 * @export
 * @class CommonComponentTestHelper
 * @template T Component type to test for example AppComponent
 * @author Kevin Guanche Darias
 */
export class CommonComponentTestHelper<T> extends AbstractCommonTestHelper<T> {
    /**
     * Will return readonly reference to component instance
     *
     * @readonly
     * @type {T}
     * @memberOf CommonComponentTestHelper
     * @throws ProgrammingError When undefined, maybe because <b> called outside an it() or beforeEach() </b>
     * @author Kevin Guanche Darias
     */
    public get component(): T {
        if (!this._component) {
            throw new ProgrammingError('Cannot get component, did you call ' + this.constructor.name + '.createComponent() ?');
        }
        return this._component;
    }
    private _component: T;

    /**
     * Will return readonly reference to component's fixture
     *
     * @readonly
     * @type {ComponentFixture<T>}
     * @memberOf CommonComponentTestHelper
     * @throws ProgrammingError When undefined, maybe because <b> called outside an it() or beforeEach() </b>
     * @author Kevin Guanche Darias
     */
    public get fixture(): ComponentFixture<T> {
        if (!this._fixture) {
            throw new ProgrammingError('Cannot get fixture, did you call' + this.constructor.name + '.createComponent() ?');
        }
        return this._fixture;
    }
    private _fixture: ComponentFixture<T>;

    /**
     * Creates an instance of CommonComponentTestHelper.
     *
     * @param {Type<T>} targetClass target component to test, for example <b>AppComponent</b>
     * @param {TestModuleMetadata} [config] Test declarations, providers... , if undefined defaults to app settings
     * @param {boolean} [fastSpawn=true] if should configure the test and create the component, just <b>NOW!</b>
     * @param {boolean} [runNgOnInit=true] Should start ng life-cycle (calls ngOnInit etc)
     * @memberOf CommonComponentTestHelper
     * @author Kevin Guanche Darias
     */
    public constructor(targetClass: Type<T>, config: TestModuleMetadata, fastSpawn = true, runNgOnInit = true) {
        super();
        this._targetClass = targetClass;
        if (fastSpawn) {
            this.configureTestingModule(config).configureServiceLocator().createComponent(runNgOnInit);
        }
    }

    /**
     * Creates the tested component itself, and it's fixture <br>
     * <b>IMPORTANT:</b> Does it <b>INSIDE</b> a beforeEach() block
     *
     * @param {boolean} [runNgOnInit=true] runs ng life-cycle, for example runs ngOnInit() method
     * @returns {CommonComponentTestHelper<T>}
     * @memberOf CommonComponentTestHelper
     * @author Kevin Guanche Darias
     */
    public createComponent(runNgOnInit = true): CommonComponentTestHelper<T> {
        beforeEach(() => {
            this.createComponentNow(runNgOnInit);
        });
        return this;
    }

    /**
     * Creates the tested component itself, and it's fixture <br>k
     *
     * @param {boolean} [runNgOnInit=true] runs ng life-cycle, for example runs ngOnInit() method
     * @returns {CommonComponentTestHelper<T>}
     * @memberOf CommonComponentTestHelper
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public createComponentNow(runNgOnInit = true): CommonComponentTestHelper<T> {
        this._fixture = TestBed.createComponent(this._targetClass);
        this._component = this._fixture.componentInstance;
        if (runNgOnInit) {
            this.startNgLifeCycle();
        }
        return this;
    }

    /**
     * Starts ng life-cycle, so runs things like ngOnInit() ... ngOnAfterViewInit(), etc <br>
     * <b>IMPORTANT:</b> usually is call inside a beforeEach() block, calling from describe() will crash
     *
     * @returns {CommonComponentTestHelper<T>}
     * @memberOf CommonComponentTestHelper
     * @author Kevin Guanche Darias
     */
    public startNgLifeCycle(): CommonComponentTestHelper<T> {
        this._fixture.detectChanges();
        return this;
    }

    public startNgLifeCycleBeforeEach(): CommonComponentTestHelper<T> {
        this.beforeEach(() => this.startNgLifeCycle());
        return this;
    }

    /**
     * Test the component creates with success
     *
     * @returns {CommonComponentTestHelper<T>}
     * @memberOf CommonComponentTestHelper
     * @author Kevin Guanche Darias
     */
    public testItCreates(): this {
        it('should create', () => {
            expect(this._component).toBeTruthy();
        });
        return this;
    }

    public testHtmlElementPresent(htmlElementSelector, negate = false, text?: string): this {
        const validationBoolean: boolean = !negate;
        if (!text) {
            text = 'HTML element with selector ' + htmlElementSelector + ' should' + (negate ? ' NOT ' : ' ') + 'exists';
        }

        expect(!!this._fixture.nativeElement.querySelector(htmlElementSelector)).toBe(validationBoolean, text);
        return this;
    }

    /**
     * Will trigger an angular event, with specified value
     *
     * @param {string} htmlElementSelector Using CSS style selector
     * @param {any} angularEventName Name of the event for example 'change'
     * @param {*} value Value to put as $event
     * @returns {Promise<any>} Promise run when fixture is stable!
     *
     * @memberOf CommonComponentTestHelper
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public triggerEventOnElement(htmlElementSelector: string, angularEventName: string, value?: any): Promise<any> {
        return new Promise((resolve, reject) => {
            const element: DebugElement = this._fixture.debugElement.query(By.css(htmlElementSelector));
            if (!element) {
                throw new ProgrammingError('No element with selector ' + htmlElementSelector + ' in the fixture');
            }

            this.fixture.whenStable().then(() => {
                const eventValue = value || {};
                eventValue.target = this._fixture.nativeElement.querySelector(htmlElementSelector);
                element.triggerEventHandler(angularEventName, eventValue);
                this.startNgLifeCycle()._fixture.detectChanges();
                this._fixture.whenStable().then(() => resolve());
            });
        });
    }
}
