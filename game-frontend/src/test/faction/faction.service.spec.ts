import { testingConfig } from '../../settings';
import { CommonServiceTestHelper } from './../../helpers/common-service-test.helper';
import { FactionService } from './../../app/faction/faction.service';


describe('FactionService', () => {
  const helper: CommonServiceTestHelper<FactionService> = new CommonServiceTestHelper(FactionService, testingConfig);
  helper.testItCreates();
});
