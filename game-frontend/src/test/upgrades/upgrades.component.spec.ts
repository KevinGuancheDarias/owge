import { UpgradeService } from '../../app/service/upgrade.service';
import { TestMetadataBuilder } from '../../helpers/test-metadata.builder';
import { async, TestModuleMetadata } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA, DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { UpgradesComponent } from '../../app/upgrades/upgrades.component';
import { CommonComponentTestHelper } from './../../helpers/common-component-test.helper';

describe('UpgradesComponent', () => {
  const testingConfig: TestModuleMetadata = new TestMetadataBuilder(false)
    .withSetDeclarations([UpgradesComponent])
    .withSetProviders([UpgradeService])
    .withDependency('BaseComponent')
    .getTestModuleMetadata();
  testingConfig.schemas = [CUSTOM_ELEMENTS_SCHEMA];
  const helper: CommonComponentTestHelper<UpgradesComponent> = new CommonComponentTestHelper(UpgradesComponent, testingConfig, true, false);
  helper.testItCreates();

  xit('should not show any obtainedUpgrade if empty or null', () => {

  });

  xit('should hide unavailable obtainedUpgrade', () => {

  });

  xit('should pass obtainedUpgrade instance to child DisplaySingleUpgrade', () => {

  });

  it('should call onRunningUpgradeDone(), when child says so', async(() => {
    spyOn(helper.component, 'ngOnInit').and.stub();
    spyOn(helper.component, 'onRunningUpgradeDone').and.stub();
    helper.component.obtainedUpgrades = <any>[{ available: true }];
    helper.fixture.detectChanges();
    const displaySingleUpgrade: DebugElement = helper.fixture.debugElement.query(By.css('app-display-single-upgrade'));
    displaySingleUpgrade.triggerEventHandler('onRunningUpgradeDone', null);
    expect(helper.component.onRunningUpgradeDone).toHaveBeenCalledTimes(1);
  }));
});
