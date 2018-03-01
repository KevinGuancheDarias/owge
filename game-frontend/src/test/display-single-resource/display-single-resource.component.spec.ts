import { testingConfig } from '../../settings';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { DisplaySingleResourceComponent } from './../../app/display-single-resource/display-single-resource.component';

describe('DisplaySingleResourceComponent', () => {
  const helper: CommonComponentTestHelper<DisplaySingleResourceComponent> = new CommonComponentTestHelper(
    DisplaySingleResourceComponent,
    testingConfig
  );
  helper.testItCreates();
});
