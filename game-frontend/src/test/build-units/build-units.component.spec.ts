import { testingConfig } from '../../settings';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { BuildUnitsComponent } from './../../app/build-units/build-units.component';

describe('BuildUnitsComponent', () => {
  const helper: CommonComponentTestHelper<BuildUnitsComponent> = new CommonComponentTestHelper(BuildUnitsComponent, testingConfig);
  helper.testItCreates();
});
