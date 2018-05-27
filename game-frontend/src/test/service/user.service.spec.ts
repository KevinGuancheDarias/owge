import { testingConfig } from '../../settings';
import { CommonServiceTestHelper } from './../../helpers/common-service-test.helper';
import { UserService } from './../../app/service/user.service';

describe('UserService', () => {
  const helper: CommonServiceTestHelper<UserService> = new CommonServiceTestHelper(UserService, true, testingConfig);
  helper.testItCreates();
});
