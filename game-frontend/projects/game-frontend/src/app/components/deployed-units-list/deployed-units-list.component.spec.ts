import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DeployedUnitsListComponent } from './deployed-units-list.component';
import { LoadingComponent } from '../../loading/loading.component';
import { ObtainedUnit } from '../../shared-pojo/obtained-unit.pojo';
import { CommonComponentTestHelper } from '../../../helpers/common-component-test.helper';
import { FormsModule } from '@angular/forms';

describe('DeployedUnitsListComponent', () => {
  const helper: CommonComponentTestHelper<DeployedUnitsListComponent> = new CommonComponentTestHelper(
    DeployedUnitsListComponent,
    {
      declarations: [LoadingComponent],
      imports: [FormsModule]
    },
  );

  const obtainedUnits: ObtainedUnit[] = [
    {
      id: 1,
      count: 6,
      unit: {
        id: 74,
        name: 'Fake Guys',
        image: '00663c30803f8d28c3a8e858650db8d4.png',
        typeId: 1
      }
    },
    {
      id: 1,
      count: 8,
      unit: {
        id: 32,
        name: 'Fake Cloned Guys',
        image: '00663c30803f8d28c3a8e858650db8d4.png',
        typeId: 1
      }
    }
  ];

  helper.testItCreates();

  it('isReady should be falsy, when obtainedUnits is not defined', () => {
    const loadingComponent: LoadingComponent = helper.findComponentInstance('owge-core-loading');
    expect(loadingComponent.isReady).toBeFalsy();
  });

  describe('When obtainedUnits is defined... ', () => {
    let loadingComponent: LoadingComponent;
    beforeEach(async done => {
      loadingComponent = helper.findComponentInstance('owge-core-loading');
      helper.component.obtainedUnits = obtainedUnits;
      helper.component.ngOnInit();
      await helper.reloadView();
      done();
    });

    it('isReady directive should be truthy', () => {
      expect(loadingComponent.isReady).toBeTruthy();
    });

    it('When no unit count has been selected, should display ONLY, the total number', () => {
      helper.testHtmlElementTextContent('.card-title', '6');
    });

    it('should not display the select unit count input, when selectable is falsy', () => {
      helper.testHtmlElementPresent('input', true);
    });

    describe('When selectable @Input is true', () => {
      beforeEach(async done => {
        helper.component.selectable = true;
        await helper.reloadView();
        done();
      });

      it('should display selected unit count input', () => {
        helper.testHtmlElementPresent('input');
      });

      it('should display selected count of total in the card title', async done => {
        const input: HTMLInputElement = document.querySelector('input');
        expect(input.value).toBe('0');
        helper.component.selectedCounts[0] = 2;
        await helper.reloadView();
        expect(input.value).toBe('2');
        helper.testHtmlElementTextContent('.card-title', '2/6');
        done();
      });

      it('should emit new selection, when we have selected, a valid unit count', async done => {
        spyOn(helper.component.selection, 'emit').and.stub();
        helper.triggerNgModel('input', '3');
        await helper.doWait();
        await helper.reloadView();
        expect(helper.component.selection.emit).toHaveBeenCalledTimes(1);
        expect(helper.component.selection.emit).toHaveBeenCalledWith([
          {
            id: 74,
            count: 3
          }
        ]);
        done();
      });
    });
  });
});
