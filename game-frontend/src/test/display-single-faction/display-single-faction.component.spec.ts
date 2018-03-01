import { testingConfig } from '../../settings';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { DisplaySingleFactionComponent } from './../../app/display-single-faction/display-single-faction.component';

describe('DisplaySingleFactionComponent', () => {
  const helper: CommonComponentTestHelper<DisplaySingleFactionComponent> = new CommonComponentTestHelper(
    DisplaySingleFactionComponent,
    testingConfig
  );
  helper.testItCreates();
});
