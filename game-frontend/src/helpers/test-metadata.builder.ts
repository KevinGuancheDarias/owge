import { ProgrammingError } from '../error/programming.error';
import { testingConfig } from '../settings';
import { Type } from '@angular/core';
import { Provider } from '@angular/core';
import { APP_BASE_HREF } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { HttpModule } from '@angular/http';
import { FormsModule } from '@angular/forms';
import { TestModuleMetadata } from '@angular/core/testing';

interface ProviderPojo {

    /**
     * Which type is provided
     *
     * @type {Type<any>|string}
     * @memberOf ProviderPojo
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    provide: Type<any> | string;

    /**
     * Instance provided to use as provider for that type
     *
     * @type {*}
     * @memberOf ProviderPojo
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    value: any;
}

export class TestMetadataBuilder {

    /**
     * This property  contains a dictionary of dependencies groups <br>
     * For example a dependency whose name is 'BaseService', will have set some providers and imports required by the BaseService <br>
     * <b>When is this useful?:</b>, when you have a component or service, from which you extend, in multiple classes
     *
     * @private
     * @static
     * @type {{ [key: string]: TestModuleMetadata }}
     * @memberOf TestMetadataBuilder
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private static dependenciesMap: { [key: string]: TestModuleMetadata } = {};

    /**
     * Resulting object of the build proccess, can be get with getTestModuleMetadata()
     *
     * @private
     * @type {TestModuleMetadata}
     * @memberOf TestMetadataBuilder
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private _testModuleMetadata: TestModuleMetadata = {
        declarations: [],
        imports: [],
        providers: []
    };

    /**
     * Will add a new dependency group to the dictionary
     *
     * @static
     * @param {string} groupName name of the group to add, for example 'BaseService'
     * @param {TestModuleMetadata} data
     *
     * @memberOf TestMetadataBuilder
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public static registerDependencyGroup(groupName: string, data: TestModuleMetadata): void {
        TestMetadataBuilder.dependenciesMap[groupName] = data;
    }

    /**
     * Will clone the testing object to a new one (avoids modifing source objects)
     *
     * @private
     * @static
     * @param {TestModuleMetadata} originalMetadata The original reference
     * @returns {TestModuleMetadata} A new instance of cloned originalMetadata
     *
     * @memberOf TestMetadataBuilder
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private static _cloneObject(originalMetadata: TestModuleMetadata): TestModuleMetadata {
        const clonedInstance: TestModuleMetadata = { declarations: [], providers: [], imports: [] };
        for (const currentMetadataType of ['declarations', 'providers', 'imports']) {
            originalMetadata[currentMetadataType].forEach(current => {
                clonedInstance[currentMetadataType].push(current);
            });
        }
        return clonedInstance;
    }

    /**
     * Creates an instance of TestMetadataBuilder.
     *
     * @param {boolean} [withDefaultInformationAdded=true] If true; will load testing information from settings.ts , defaults to true
     *
     * @memberOf TestMetadataBuilder
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public constructor(withDefaultInformationAdded = true) {
        if (withDefaultInformationAdded) {
            this._testModuleMetadata = TestMetadataBuilder._cloneObject(testingConfig);
        }
    }

    public getTestModuleMetadata(): TestModuleMetadata {
        return this._testModuleMetadata;
    }

    public withSetDeclarations(declarations: any[]): TestMetadataBuilder {
        this._testModuleMetadata.declarations = declarations;
        return this;
    }

    public withSetImports(imports: any[]): TestMetadataBuilder {
        this._testModuleMetadata.imports = imports;
        return this;
    }

    public withSetProviders(providers: any[]): TestMetadataBuilder {
        this._testModuleMetadata.providers = providers;
        return this;
    }

    public withAppendDeclarations(declarations: any[]): TestMetadataBuilder {
        this._testModuleMetadata.declarations = this._testModuleMetadata.declarations.concat(declarations);
        return this;
    }

    public withAppendImports(imports: any[]): TestMetadataBuilder {
        this._testModuleMetadata.imports = this._testModuleMetadata.imports.concat(imports);
        return this;
    }

    public withAppendProviders(providers: any[]): TestMetadataBuilder {
        this._testModuleMetadata.providers = this._testModuleMetadata.providers.concat(providers);
        return this;
    }

    public withAppendMockProviders(providers: ProviderPojo[]): TestMetadataBuilder {
        const providersToAppend: any[] = [];
        providers.forEach(current => {
            providersToAppend.push({ provide: current.provide, useValue: current.value });
        });
        this._testModuleMetadata.providers = this._testModuleMetadata.providers.concat(providersToAppend);
        return this;
    }

    /**
     * Will append the declarations,imports, and providers of the dependency group, to the resulting object
     *
     * @param {string} groupName For example 'BaseService'
     * @returns {TestMetadataBuilder}
     *
     * @memberOf TestMetadataBuilder
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public withDependency(groupName: string): TestMetadataBuilder {
        const dependencyData: TestModuleMetadata = TestMetadataBuilder.dependenciesMap[groupName];
        if (!dependencyData) {
            throw new ProgrammingError('TestMetadatarBuilder: Dependency group with name ' + groupName + ' not found');
        }
        this._testModuleMetadata.declarations = this._testModuleMetadata.declarations.concat(dependencyData.declarations);
        this._testModuleMetadata.imports = this._testModuleMetadata.imports.concat(dependencyData.imports);
        this._testModuleMetadata.providers = this._testModuleMetadata.providers.concat(dependencyData.providers);
        return this;
    }

    public withHttp(): TestMetadataBuilder {
        this._testModuleMetadata.imports = this._testModuleMetadata.imports.concat(HttpModule);
        return this;
    }

    public withForms(): TestMetadataBuilder {
        this._testModuleMetadata.imports = this._testModuleMetadata.imports.concat(FormsModule);
        return this;
    }

    public withRouter(routes: Routes = []): TestMetadataBuilder {
        this._testModuleMetadata.providers = this._testModuleMetadata.providers.concat([{ provide: APP_BASE_HREF, useValue: '/' }]);
        this._testModuleMetadata.imports = this._testModuleMetadata.imports.concat(RouterModule.forRoot(routes));
        return this;
    }
}
