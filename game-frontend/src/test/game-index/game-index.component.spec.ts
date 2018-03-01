import { async } from '@angular/core/testing';
import { testingConfig } from '../../settings';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';
import { GameIndexComponent } from './../../app/game-index/game-index.component';

describe('GameIndexComponent', () => {
  const helper: CommonComponentTestHelper<GameIndexComponent> = new CommonComponentTestHelper(
    GameIndexComponent,
    testingConfig
  );
  helper.testItCreates();

  it('should display Hello World when userData is defined', async(() => {
    helper.component['_userData'] = <any>{};
    helper.fixture.detectChanges();
    const nativeElement: HTMLElement = helper.fixture.nativeElement;
    const found = nativeElement.querySelector('.game-index');
    expect(found).toBeTruthy();
  }));
});
