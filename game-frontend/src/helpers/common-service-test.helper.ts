import { ServiceLocator } from './../app/service-locator/service-locator';
import { TestModuleMetadata } from '@angular/core/testing';
import { AbstractCommonTestHelper } from './abstract-common-test.helper';
import { ProgrammingError } from './../error/programming.error';
import { Type } from '@angular/core';

export class CommonServiceTestHelper<T> extends AbstractCommonTestHelper<T> {

    /**
     * Represents the service instance <br>
     * <b>IMPORTANT:</b> Available after createService has been called
     * <b>READONLY!</b>
     *
     * @readonly
     * @type {T}
     * @memberOf AbstractCommonTestHelper
     * @throws ProgrammingError When undefined, maybe because <b> called outside an it() or beforeEach() </b>
     * @author Kevin Guanche Darias
     */
    public get serviceInstance(): T {
        if (!this._serviceInstance) {
            throw new ProgrammingError('Cannot get service, did you call' + this.constructor.name + '.createService() ?');
        }
        return this._serviceInstance;
    }
    private _serviceInstance: T;

    /**
     * Creates an instance of CommonServiceTestHelper.
     *
     * @param {Type<T>} targetClass The target class
     * @param {boolean} [fastSpawn=true] Configures TestBed and automatically create the service, and defines ServiceLocator's injector
     * @param {TestModuleMetadata} [config] Declarations, providers, etc, defaults to app settings
     *
     * @memberOf CommonServiceTestHelper
     * @author Kevin Guanche Darias
     */
    public constructor(targetClass: Type<T> | string, fastSpawn = true, config?: TestModuleMetadata) {
        super(targetClass);
        if (fastSpawn) {
            this.configureTestingModule(config).configureServiceLocator().createService();
        }
    }

    /**
     * Creates the tested service itself
     *
     * @returns {this}
     *
     * @memberOf CommonServiceTestHelper
     * @author Kevin Guanche Darias
     */
    public createService(): this {
        this.beforeEach(() => {
            this._serviceInstance = ServiceLocator.injector.get(this._targetClass);
        });
        return this;
    }

    /**
     * Test that the service creates with success
     *
     * @returns {this}
     *
     * @memberOf CommonServiceTestHelper
     * @author Kevin Guanche Darias
     */
    public testItCreates(): this {
        it('should create', () => {
            expect(this._serviceInstance).toBeTruthy();
        });
        return this;
    }
}
