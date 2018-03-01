import { testingConfig } from '../../settings';
import { CommonServiceTestHelper } from './../../helpers/common-service-test.helper';
import { UnitService } from './../../app/service/unit.service';

describe('UnitService', () => {
  const helper: CommonServiceTestHelper<UnitService> = new CommonServiceTestHelper(UnitService, testingConfig);
  helper.testItCreates();
});
