import { testingConfig } from '../../settings';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { DisplaySingleUniverseComponent } from './../../app/display-single-universe/display-single-universe.component';

describe('DisplaySingleUniverseComponent', () => {
  const helper: CommonComponentTestHelper<DisplaySingleUniverseComponent> = new CommonComponentTestHelper(
    DisplaySingleUniverseComponent,
    testingConfig
  );
  helper.testItCreates();
});
