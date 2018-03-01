import { testingConfig } from '../../settings';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { UniverseSelectionComponent } from './../../app/universe-selection/universe-selection.component';

describe('UniverseSelectionComponent', () => {
  const helper: CommonComponentTestHelper<UniverseSelectionComponent> = new CommonComponentTestHelper(
    UniverseSelectionComponent,
    testingConfig
  );
  helper.testItCreates();
});
