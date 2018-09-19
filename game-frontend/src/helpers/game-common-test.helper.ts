import { ProgrammingError } from './../error/programming.error';
import { CommonServiceTestHelper } from './common-service-test.helper';
import { CommonComponentTestHelper } from './common-component-test.helper';
import { PlanetPojo } from './../app/shared-pojo/planet.pojo';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { TestBed } from '@angular/core/testing';
import { LoginSessionService } from './../app/login-session/login-session.service';
import { Universe } from '../app/shared-pojo/universe.pojo';
import { AbstractCommonTestHelper } from './abstract-common-test.helper';
import { HttpHeaders } from '@angular/common/http';
import { GameBaseService } from '../app/service/game-base.service';
import { Observable } from 'rxjs/Observable';
import { UserPojo } from '../app/shared-pojo/user.pojo';

export class GameCommonTestHelper<T = any> {

    private _universe: Universe = {
        id: 2,
        name: 'World Of Test',
        restBaseUrl: '/fakeverse'
    };

    /**
     * Contains the commn-helper, this can be of type component or service
     *
     * @private
     * @type {T} CommonComponentTestHelper or CommonServiceTestHelper
     * @memberOf GameCommonTestHelper
     */
    private _encapsulatedHelper: CommonComponentTestHelper<T> | CommonServiceTestHelper<T>;

    public constructor(helper: T) {
        if (!(helper instanceof CommonComponentTestHelper) && !(helper instanceof CommonServiceTestHelper)) {
            throw new ProgrammingError('Helper class must be CommonComponentTestHelper or CommonServiceTestHelper,\
                 but it is ' + helper.constructor.name);
        }
        this._encapsulatedHelper = <any>helper;
    }

    public getEncapsulatedHelper(): CommonComponentTestHelper<T> | CommonServiceTestHelper<T> {
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



    /**
     * Fakes the request to doGetWithAuthorization
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {Function} action
     * @param {GameBaseService} [service]
     * @since 0.6.0
     * @memberof GameCommonTestHelper
     */
    public fakeDoGetWithAuthorizationToGame(action: Function, service?: GameBaseService) {
        const spiedService = service
            ? service
            : (<any>this._encapsulatedHelper).serviceInstance;
        spyOn((<any>spiedService), 'doGetWithAuthorizationToGame').and.returnValue((<any>Observable).of(action()));
    }

    public getUniverse(): Universe {
        return this._universe;
    }

    /**
     * Mocks the getSelectedUniverse() of LoginSessionService
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @returns {this}
     * @memberof GameCommonTestHelper
     */
    public mockUniverse(): this {
        const service: LoginSessionService = TestBed.get(LoginSessionService);
        this._encapsulatedHelper.spyOn(service, 'getSelectedUniverse').and.returnValue(this._universe);
        return this;
    }

    public mockUserTokenData(user: UserPojo): void {
        const service: LoginSessionService = this._getLogginSessionService();
        spyOn(service, 'findTokenData').and.returnValue(user);
    }

    public mockGetHttpClientHeaders(): this {
        const service: LoginSessionService = TestBed.get(LoginSessionService);
        this._encapsulatedHelper
            .spyOn(service, 'genHttpClientHeaders')
            .and.returnValue(new HttpHeaders().append('Authorization', 'Bearer fake'));
        return this;
    }

    public expectHttpClientHeaders(headers: HttpHeaders): this {
        expect(headers.get('Authorization')).toBe('Bearer fake');
        return this;
    }

    private _getLogginSessionService(): LoginSessionService {
        return TestBed.get(LoginSessionService);
    }
}
