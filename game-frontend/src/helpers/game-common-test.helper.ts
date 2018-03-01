import { ProgrammingError } from './../error/programming.error';
import { CommonServiceTestHelper } from './common-service-test.helper';
import { CommonComponentTestHelper } from './common-component-test.helper';
import { PlanetPojo } from './../app/shared-pojo/planet.pojo';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { TestBed } from '@angular/core/testing';
import { LoginSessionService } from './../app/login-session/login-session.service';

export class GameCommonTestHelper<T> {

    /**
     * Contains the commn-helper, this can be of type component or service
     *
     * @private
     * @type {T} CommonComponentTestHelper or CommonServiceTestHelper
     * @memberOf GameCommonTestHelper
     */
    private _encapsulatedHelper: T;

    public constructor(helper: T) {
        if (!(helper instanceof CommonComponentTestHelper) && !(helper instanceof CommonServiceTestHelper)) {
            throw new ProgrammingError('Helper class must be CommonComponentTestHelper or CommonServiceTestHelper,\
                 but it is ' + helper.constructor.name);
        }
        this._encapsulatedHelper = <any>helper;
    }

    public getEncapsulatedHelper(): T {
        return this._encapsulatedHelper;
    }

    /**
     *  Fakes the response from LoginSessionService.isInGame Observable <br>
     * NOTICE: For unknown reasons, this subject won't fire a view change event (take care!)
     *
     * @param {boolean} isInGame Value that the service should return
     * @returns {this}
     * @memberOf GameCommonTestHelper
     * @todo In the future replace with spyOnProperty() when added to the master branch of jasmine
     * @see https://github.com/jasmine/jasmine/pull/1008
     * @author Kevin Guanche Darias
     */
    public fakeLoginSessionServiceIsInGame(isInGame = true): this {
        const subject: BehaviorSubject<boolean> = new BehaviorSubject(isInGame);
        (<any>this._getLogginSessionService())._isInGame = subject;
        return this;
    }

    public fakeLoginSessionServiceFindSelectedPlanet(selectedPlanet?: PlanetPojo): this {
        const subject: BehaviorSubject<PlanetPojo> = new BehaviorSubject(selectedPlanet);
        (<any>this._getLogginSessionService())._findSelectedPlanet = subject;
        return this;
    }

    private _getLogginSessionService(): LoginSessionService {
        return TestBed.get(LoginSessionService);
    }
}
