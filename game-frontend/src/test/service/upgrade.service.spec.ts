import { testingConfig } from '../../settings';
import { CommonServiceTestHelper } from './../../helpers/common-service-test.helper';
import { UpgradeService } from './../../app/service/upgrade.service';

describe('UpgradeService', () => {
  const helper: CommonServiceTestHelper<UpgradeService> = new CommonServiceTestHelper(UpgradeService, testingConfig);
  helper.testItCreates();
});
