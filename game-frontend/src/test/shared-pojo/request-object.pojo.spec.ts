import { Config } from '../../app/config/config.pojo';
import { RequestObject } from '../../pojo/request-object.pojo';
import { URLSearchParams } from '@angular/http';

describe('RequestObjectPojo...', () => {
    it('should use config to define default headers, if not passed', () => {
        spyOn(Config, 'genCommonFormUrlencoded').and.stub();
        const uRLSearchParams = new URLSearchParams();
        const requestObject = new RequestObject(uRLSearchParams, null);
        expect(Config.genCommonFormUrlencoded).toHaveBeenCalledTimes(1);
    });
});
