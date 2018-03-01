import { testingConfig } from '../../settings';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { DeployedUnitsBigComponent } from './../../app/deployed-units-big/deployed-units-big.component';

describe('DeployedUnitsBigComponent', () => {
  const helper: CommonComponentTestHelper<DeployedUnitsBigComponent> = new CommonComponentTestHelper(
    DeployedUnitsBigComponent,
    testingConfig
  );
  helper.testItCreates();
});
