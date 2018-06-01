import { By } from '@angular/platform-browser';
import { AbstractCommonTestHelper, StatePrefix } from './abstract-common-test.helper';
import { ProgrammingError } from './../error/programming.error';
import { TestBed, TestModuleMetadata, ComponentFixture } from '@angular/core/testing';
import { DebugElement, Type, SimpleChanges, SimpleChange } from '@angular/core';


/**
 * Represents a component and its HTMLElement
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface ComponentElement
 * @template T Component type
 */
export interface ComponentElement<T> {
    component: T;
    element: HTMLElement;
}

export interface DirectiveNameValue {
    name: string;
    value: any;
}

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

    protected _targetClass: Type<T>;

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
    public constructor(targetClass: Type<T>, config?: TestModuleMetadata, fastSpawn = true, runNgOnInit = true) {
        super(targetClass);
        let targetConfig: TestModuleMetadata = config;
        if (config && config.declarations instanceof Array) {
            targetConfig.declarations = config.declarations.concat([targetClass]);
        } else {
            targetConfig = {
                declarations: [targetClass]
            };
        }

        if (fastSpawn) {
            this.configureTestingModule(targetConfig).configureServiceLocator().createComponent(runNgOnInit);
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
     * Test the number of HTML elements with the given CSS selector match the <b>expectedNumber</b>
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {string} htmlElementSelector CSS selector
     * @param {number} expectedNumber Number of elements that should be matched
     * @returns {HTMLElement[]} elements that were found
     * @memberof CommonComponentTestHelper
     */
    public testHtmlNumberOfElements(htmlElementSelector: string, expectedNumber: number): HTMLElement[] {
        const retVal: HTMLElement[] = this.fixture.nativeElement.querySelectorAll(htmlElementSelector);
        this.testHtmlElementPresent(htmlElementSelector);
        expect(retVal.length).toBe(expectedNumber);
        return Array.prototype.slice.call(retVal);
    }

    /**
     * Validates the text node value of the HTML element, if It's an INPUT will validate it't value
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {string} htmlElementSelector target HTML element CSS like selector
     * @param {string} expectedValue Expected text value
     * @param {boolean} [negate=false] If true will test that element text is NOT the same as <i>expectedValue</i>
     * @returns {this}
     * @memberof CommonComponentTestHelper
     */
    public testHtmlElementTextContent(htmlElementSelector: string, expectedValue: string, negate = false): this {
        this.testHtmlElementPresent(htmlElementSelector);
        const validationBoolean: boolean = !negate;
        const element: HTMLElement = this._fixture.nativeElement.querySelector(htmlElementSelector);
        const targetProperty: keyof HTMLInputElement = element instanceof HTMLInputElement
            ? 'value'
            : 'innerText';
        if (negate) {
            expect(element[targetProperty]).not.toBe(expectedValue);
        } else {
            expect(element[targetProperty]).toBe(expectedValue);
        }
        return this;
    }

    /**
     * Fires a dom value change event in a input/textarea element, useful for testing that ngModel is specified
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {string} htmlElementSelector CSS selector
     * @param {string} value Value to assign to the input
     * @returns {Promise<void>} Resolves when the view is stable
     * @memberof CommonComponentTestHelper
     * @throws {ProgrammingError} When element is not an input nor a textarea
     */
    public async fireInputValueChangeEvent(htmlElementSelector: string, value: string): Promise<void> {
        this.testHtmlElementPresent(htmlElementSelector);
        const input: HTMLInputElement | HTMLTextAreaElement = this._fixture.debugElement.query(By.css(htmlElementSelector)).nativeElement;
        if (input.tagName !== 'INPUT' && input.tagName !== 'TEXTAREA') {
            throw new ProgrammingError('element must be an input, but element tagName is ' + input.tagName);
        }
        input.value = value;
        if (input instanceof HTMLTextAreaElement) {
            input.innerText = value;
        }
        input.dispatchEvent(new Event('input'));
        await this.reloadView();
    }

    /**
     * Fires a select event
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {string} htmlElementSelector target select element
     * @param {string} optionValue target option value
     * @param {boolean} [fakeOption=false] if true, option element will be fake, so there is no need for it to exists
     * @returns {Promise<void>}
     * @memberof CommonComponentTestHelper
     */
    public async fireSelectValueChangeEvent(htmlElementSelector: string, optionValue: string, fakeOption = false): Promise<void> {
        this.testHtmlElementPresent(htmlElementSelector);
        const select: HTMLSelectElement = this._fixture.debugElement.query(By.css(htmlElementSelector)).nativeElement;
        if (select.tagName !== 'SELECT') {
            throw new ProgrammingError('element MUST be a select, but element tagName is ' + select.tagName);
        }
        if (fakeOption) {
            const fakeOptionEl: HTMLOptionElement = document.createElement('option');
            fakeOptionEl.value = optionValue;
            select.options.add(fakeOptionEl);
        }

        const index = Array.prototype.findIndex.call(select.options, (current: HTMLOptionElement) => current.value === optionValue);
        if (index === -1) {
            throw new ProgrammingError('There is no option with value ' + optionValue);
        }
        select.value = (<any>select.options[index]).value;
        select.selectedIndex = index;
        select.dispatchEvent(new Event('change'));
        await this.reloadView();
        if (fakeOption) {
            select.options.remove(select.options.length - 1);
        }
    }

    /**
     * Finds the value of a directive
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {string} htmlElementSelector
     * @param {string} directiveSelector
     * @returns {*}
     * @memberof CommonComponentTestHelper
     */
    public findDirectiveValue(htmlElementSelector: string, directiveSelector: string): any {
        const target: DebugElement = this._fixture.debugElement.query(By.css(htmlElementSelector));
        if (target) {
            return target.properties[directiveSelector];
        } else {
            console.warn('No component with id ' + htmlElementSelector + ' was found');
        }
    }

    /**
     * Test a directive value <br>
     * <b>Notice: This must be called when the view is stable <br>
     * <b>Seems to work ONLY when the target-selector is a "custom schema" (selector is not in the "declarations" of the TestBed</b> <br>
     * <b>If called is a declared component you can use something like </b> <code>
     *  expect(CommonComponentTestHelper.findComponentInstance(htmlElementSelector)[directiveSelector]).toBe(expectedValue)
     *  </code>
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {string} htmlElementSelector CSS selector for element having the directive
     * @param {string} directiveSelector Directive to test
     * @param {*} expectedValue Expected value for the directive
     * @returns {CommonComponentTestHelper} this instance
     * @memberof CommonComponentTestHelper
     */
    public testDirectiveValue(htmlElementSelector: string, directiveSelector: string, expectedValue: any): this {
        const actualValue = this.findDirectiveValue(htmlElementSelector, directiveSelector);
        expect(actualValue).toBe(
            expectedValue,
            'directive [' + directiveSelector + '] for CSS selector "' + htmlElementSelector + '" has value: "' + actualValue +
            '", but should be "' + expectedValue + '"'
        );
        return this;
    }

    public itDirectivesForSelector(selector: string, ...directives: DirectiveNameValue[]): void {
        directives.forEach(current => {
            it(`Should pass ${current.name} to ${selector}`, () => {
                this.testDirectiveValue(selector, current.name, current.value);
            });
        });
    }

    /**
     * Reloads the view, this method is a shortcut to calling this.startNgLifeCycle() and fixture.whenStable()
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @returns {Promise<void>} Solves when the view is stable
     * @memberof CommonComponentTestHelper
     */
    public async reloadView(): Promise<void> {
        await this.startNgLifeCycle()._fixture.whenStable();
    }

    /**
     * Will trigger an angular event, with specified value
     *
     * @param {string} htmlElementSelector Using CSS style selector
     * @param {any} angularEventName Name of the event for example 'change'
     * @param {*} value Value to put as $event
     * @returns {Promise<this>} Promise run when fixture is stable!
     *
     * @memberOf CommonComponentTestHelper
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public triggerEventOnElement(htmlElementSelector: string, angularEventName: string, value?: any): Promise<this> {
        return new Promise((resolve, reject) => {
            const element: DebugElement = this._fixture.debugElement.query(By.css(htmlElementSelector));
            if (!element) {
                throw new ProgrammingError('No element with selector ' + htmlElementSelector + ' in the fixture');
            }

            this.fixture.whenStable().then(() => {
                const eventValue = value || {};
                if (!eventValue.target) {
                    eventValue.target = this._fixture.nativeElement.querySelector(htmlElementSelector);
                }
                element.triggerEventHandler(angularEventName, eventValue);
                this.startNgLifeCycle()._fixture.detectChanges();
                this._fixture.whenStable().then(() => resolve());
            });
        });
    }

    /**
     * Finds a component instance <br>
     * <b>IMPORTANT: If the component selector is NOT in the declarations, will have unexpected behavior </b>
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @returns {any} Angular component instance
     * @param htmlElementSelector selector of the component
     */
    public findComponentInstance(htmlElementSelector: string): any {
        return this.fixture.debugElement.query(By.css(htmlElementSelector)).componentInstance;
    }

    /**
     * Finds all component instances for given selector <br>
     *
     * <b>IMPORTANT: If the component selector is NOT in the declarations, will have unexpected behavior </b>
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param htmlElementSelector CSS selector
     * @template C Component type
     * @returns {C[]}  array of component instances
     */
    public findComponentInstances<C>(htmlElementSelector: string): C[] {
        return this.fixture.debugElement.queryAllNodes(By.css(htmlElementSelector))
            .filter(current => current instanceof DebugElement && !!current.componentInstance)
            .map(current => current.componentInstance);
    }

    /**
     * Finds a component instance using Angular id
     * <b>IMPORTANT: If the component selector is NOT in the declarations, will have unexpected behavior </b>
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {string} angularId
     * @param {boolean} returnElement If defined will return both the element and the component
     * @returns {any} If returnElement is true, will look like { component: Type, element: HTMLElement}, else will only return component
     * @template C Component
     * @memberof CommonComponentTestHelper
     */
    public findComponentInstanceByAngularId<C = any>(angularId: string): ComponentElement<C> {
        const el: HTMLElement = this.findElementByAngularId(angularId);
        const tempId: string = 't' + new Date().getTime().toString();
        const oldId = el.id;
        el.id = tempId;
        const retVal: ComponentElement<C> = {
            component: this.findComponentInstance('#' + tempId),
            element: el
        };
        el.id = oldId;
        return retVal;
    }

    public testInstanceOf(expectedClass: Function, state: StatePrefix = '') {
        this.testAsync('should be an instance of ' + expectedClass.name, () => {
            expect(this.component).toEqual(jasmine.any(expectedClass));
        }, state);
    }

    public findElementByAngularId(angularId): HTMLElement {
        return this.fixture.debugElement.query(de => de.references[angularId]).nativeElement;
    }

    /**
     * Fires the ngOnChanges angular life-cycle <br>
     * Due to a change in Angular 4, the fixture.detectChanges() doesn't fire the ngOnChanges() method
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {keyof T} property target property
     * @param {*} value new value
     * @returns {Promise<void>} result, it's a promise to keep an eye to the future, because in the future, will call detectChanges()
     * @see https://stackoverflow.com/questions/48086130/ngonchanges-not-called-in-angular-4-unit-test-detectchanges
     * @memberof CommonComponentTestHelper
     */
    public async fireNgChanges(property: keyof T, value: any): Promise<void> {
        const target: SimpleChanges = {};
        target[property] = new SimpleChange(this.component[property], value, null);
        this.component[property] = value;
        await this.component['ngOnChanges'](target);
    }

    /**
     * Test that fields are correctly displayed in the row <br>
     * <b>NOTICE:</b> Silently uses input fields to string
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {HTMLElement} row
     * @param {any[]} fields Expected field values, sorted from left to right
     * @memberof CommonComponentTestHelper
     */
    public testTableRowValues(row: HTMLElement, fields: any[]): void {
        if (row.tagName !== 'TR') {
            throw new ProgrammingError('Input MUST be a row item');
        }
        const stringFields = fields.map(current => current.toString());
        const tds = row.querySelectorAll('td');
        for (let i = 0; i < tds.length; i++) {
            expect(tds.item(i).textContent).toBe(stringFields[i], 'fatal, in field index ' + i);
        }
    }

    public triggerNgModel(input: HTMLInputElement | string, value: string) {
        const targetEl: HTMLInputElement = typeof input === 'string'
            ? <any>document.querySelector(input)
            : input;
        targetEl.value = value;
        targetEl.dispatchEvent(new Event('input'));
    }
}
