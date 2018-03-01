import { testingConfig } from '../../settings';
import { UserPojo } from '../../app/shared-pojo/user.pojo';
import { TokenPojo } from '../../app/login-session/token.pojo';
import { CommonServiceTestHelper } from './../../helpers/common-service-test.helper';
import { LoginSessionService } from './../../app/login-session/login-session.service';

import { TestBed, async, inject } from '@angular/core/testing';

describe('LoginSessionService', () => {
    const rawToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkYXRhIjp7fSwiaWF0IjoxOTcyNzMsImV4cCI6OTI3ODI3MSwi' +
        'c3ViIjo4MjY3MzcyNjN9.gUTTHpWrjYboBNhhPSP4xn3L7xTHzQUcuKRy4Do7ENQ';
    const token: TokenPojo = {
        data: new UserPojo(),
        iat: 197273,
        exp: 9278271,
        sub: 826737263
    };

    const helper: CommonServiceTestHelper<LoginSessionService> = new CommonServiceTestHelper(LoginSessionService, testingConfig);
    helper.testItCreates();

    helper.testAsync('findTokenData() should return data if token exists and is not expired', () => {
        spyOn(helper.serviceInstance, 'getRawToken').and.returnValue(rawToken);
        spyOn(helper.serviceInstance, 'getParsedToken').and.returnValue(token);
        spyOn(TokenPojo, 'isExpired').and.returnValue(false);

        const result: UserPojo = helper.serviceInstance.findTokenData();
        expect(result).toBeTruthy();
        expect(result).toEqual(jasmine.any(UserPojo));
    });

    helper.testAsync('findTokenData() should not return when token is expired', () => {
        spyOn(helper.serviceInstance, 'getRawToken').and.returnValue(rawToken);
        spyOn(helper.serviceInstance, 'getParsedToken').and.returnValue(token);
        spyOn(TokenPojo, 'isExpired').and.returnValue(true);

        expect(helper.serviceInstance.findTokenData()).toBeFalsy();
    });

    helper.testAsync('findTokenData() should not return when token is not in localStorage', () => {
        spyOn(helper.serviceInstance, 'getRawToken').and.returnValue(undefined);
        spyOn(helper.serviceInstance, 'getParsedToken').and.returnValue(token);
        spyOn(TokenPojo, 'isExpired').and.returnValue(false);

        expect(helper.serviceInstance.findTokenData()).toBeFalsy();
    });

    it('setTokenPojo should set the pojo to localStorage', () => {
        spyOn(window.sessionStorage, 'setItem').and.stub;
        helper.serviceInstance.setTokenPojo('someValue');
        expect(window.sessionStorage.setItem).toHaveBeenCalledWith(LoginSessionService.LOCAL_STORAGE_TOKEN_PARAM, 'someValue');
    });

    it('getParsedToken() should parse the token properly', () => {
        spyOn(window.sessionStorage, 'getItem').and.returnValue(rawToken);
        const parsedToken: TokenPojo = helper.serviceInstance.getParsedToken();
        expect(parsedToken).toBeTruthy();
        expect(parsedToken.sub).toBe(token.sub);

    });
});
