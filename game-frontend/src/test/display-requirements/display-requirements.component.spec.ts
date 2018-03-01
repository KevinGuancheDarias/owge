import { testingConfig } from '../../settings';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { DisplayRequirementsComponent } from './../../app/display-requirements/display-requirements.component';

describe('DisplayRequirementsComponent', () => {
  const helper: CommonComponentTestHelper<DisplayRequirementsComponent> = new CommonComponentTestHelper(
    DisplayRequirementsComponent,
    testingConfig
  );
  helper.testItCreates();
});
