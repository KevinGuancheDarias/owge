import { testingConfig } from '../../settings';
import { UnitPojo } from './../../app/shared-pojo/unit.pojo';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { DisplaySingleUnitComponent } from './../../app/display-single-unit/display-single-unit.component';

describe('DisplaySingleUnitComponent', () => {
  const helper: CommonComponentTestHelper<DisplaySingleUnitComponent> = new CommonComponentTestHelper(
    DisplaySingleUnitComponent,
    testingConfig,
    true,
    false
  );

  helper.beforeEach(() => helper.component.unit = new UnitPojo).startNgLifeCycleBeforeEach()
    .testItCreates();
});
