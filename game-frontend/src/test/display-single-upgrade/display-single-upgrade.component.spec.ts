import { testingConfig } from '../../settings';
import { Upgrade } from './../../app/shared-pojo/upgrade.pojo';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { DisplaySingleUpgradeComponent } from './../../app/display-single-upgrade/display-single-upgrade.component';

describe('DisplaySingleUpgradeComponent', () => {
  const helper: CommonComponentTestHelper<DisplaySingleUpgradeComponent> = new CommonComponentTestHelper(
    DisplaySingleUpgradeComponent,
    testingConfig,
    true,
    false
  );
  helper.beforeEach(() => helper.component.upgrade = new Upgrade()).startNgLifeCycleBeforeEach().testItCreates();
});
