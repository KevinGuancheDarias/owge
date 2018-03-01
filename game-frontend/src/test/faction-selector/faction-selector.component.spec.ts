import { testingConfig } from '../../settings';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { FactionSelectorComponent } from './../../app/faction-selector/faction-selector.component';

describe('FactionSelectorComponent', () => {
  const helper: CommonComponentTestHelper<FactionSelectorComponent> = new CommonComponentTestHelper(
    FactionSelectorComponent,
    testingConfig
  );
  helper.testItCreates();
});
