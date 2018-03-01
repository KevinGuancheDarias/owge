import { testingConfig } from '../../settings';
import { CommonServiceTestHelper } from './../../helpers/common-service-test.helper';
import { UniverseService } from './../../app/universe/universe.service';

describe('UniverseService', () => {
  const helper: CommonServiceTestHelper<UniverseService> = new CommonServiceTestHelper(UniverseService, testingConfig);
  helper.testItCreates();
});
