import { testingConfig } from '../../settings';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { DisplayDynamicImageComponent } from './../../app/display-dynamic-image/display-dynamic-image.component';

describe('DisplayDynamicImageComponent', () => {
  const helper: CommonComponentTestHelper<DisplayDynamicImageComponent> = new CommonComponentTestHelper(
    DisplayDynamicImageComponent,
    testingConfig
  );
  helper.testItCreates();
});
