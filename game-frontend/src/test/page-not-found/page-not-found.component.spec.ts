import { testingConfig } from '../../settings';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { PageNotFoundComponent } from './../../app/page-not-found/page-not-found.component';

describe('PageNotFoundComponent', () => {
  const helper: CommonComponentTestHelper<PageNotFoundComponent> = new CommonComponentTestHelper(
    PageNotFoundComponent,
    testingConfig,
    true,
    false
  );
  helper.withMockRouterBeforeEach().startNgLifeCycleBeforeEach().testItCreates();
});
