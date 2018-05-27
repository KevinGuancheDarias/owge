import { testingConfig } from '../../settings';
import { CommonServiceTestHelper } from './../../helpers/common-service-test.helper';
import { ResourceManagerService } from './../../app/service/resource-manager.service';

describe('ResourceManagerService', () => {
  const helper: CommonServiceTestHelper<ResourceManagerService> = new CommonServiceTestHelper(
    ResourceManagerService,
    true,
    testingConfig
  );
  helper.testItCreates();
});
