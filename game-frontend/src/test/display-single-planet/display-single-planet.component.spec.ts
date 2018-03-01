import { testingConfig } from '../../settings';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { DisplaySinglePlanetComponent } from './../../app/display-single-planet/display-single-planet.component';

describe('DisplaySinglePlanetComponent', () => {
  const helper: CommonComponentTestHelper<DisplaySinglePlanetComponent> = new CommonComponentTestHelper(
    DisplaySinglePlanetComponent,
    testingConfig
  );
  helper.testItCreates();
});
