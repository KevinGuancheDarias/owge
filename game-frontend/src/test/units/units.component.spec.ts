import { testingConfig } from '../../settings';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { UnitsComponent } from './../../app/units/units.component';

describe('UnitsComponent', () => {
  const helper: CommonComponentTestHelper<UnitsComponent> = new CommonComponentTestHelper(UnitsComponent, testingConfig);
  helper.testItCreates();
});
