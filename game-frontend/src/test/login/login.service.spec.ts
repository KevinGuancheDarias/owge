import { testingConfig } from '../../settings';
import { CommonServiceTestHelper } from './../../helpers/common-service-test.helper';
import { LoginService } from './../../app/login/login.service';

describe('LoginService', () => {
  const helper: CommonServiceTestHelper<LoginService> = new CommonServiceTestHelper(LoginService, testingConfig);
  helper.testItCreates();
});
