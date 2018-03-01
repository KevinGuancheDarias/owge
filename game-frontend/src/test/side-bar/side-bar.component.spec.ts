import { testingConfig } from '../../settings';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { SideBarComponent } from './../../app/side-bar/side-bar.component';

describe('SideBarComponent', () => {
  const helper: CommonComponentTestHelper<SideBarComponent> = new CommonComponentTestHelper(SideBarComponent, testingConfig);
  helper.testItCreates();
});
