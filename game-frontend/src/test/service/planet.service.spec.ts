import { testingConfig } from '../../settings';
import { CommonServiceTestHelper } from './../../helpers/common-service-test.helper';
import { PlanetService } from './../../app/service/planet.service';

describe('PlanetService', () => {
  const helper: CommonServiceTestHelper<PlanetService> = new CommonServiceTestHelper(PlanetService, true, testingConfig);
  helper.testItCreates();
});
