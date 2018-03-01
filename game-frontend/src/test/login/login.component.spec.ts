import { testingConfig } from '../../settings';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { LoginComponent } from './../../app/login/login.component';

describe('LoginComponent', () => {
  const helper: CommonComponentTestHelper<LoginComponent> = new CommonComponentTestHelper(
    LoginComponent,
    testingConfig,
    true,
    false);
  helper.startNgLifeCycleBeforeEach().testItCreates();
});
