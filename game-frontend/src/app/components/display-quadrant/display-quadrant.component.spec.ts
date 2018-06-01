import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DisplayQuadrantComponent } from './display-quadrant.component';
import { GameCommonTestHelper } from '../../../helpers/game-common-test.helper';
import { CommonComponentTestHelper, ComponentElement } from '../../../helpers/common-component-test.helper';
import { UnitService } from '../../service/unit.service';
import { PlanetDisplayNamePipe } from '../../pipes/planet-display-name/planet-display-name.pipe';
import { FormsModule } from '@angular/forms';
import { LoginSessionService } from '../../login-session/login-session.service';
import { PlanetPojo } from '../../shared-pojo/planet.pojo';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { FakeClass } from '../../../helpers/fake-class';
import { ModalComponent } from '../modal/modal.component';
import { of } from 'rxjs/observable/of';
import { ObtainedUnit } from '../../shared-pojo/obtained-unit.pojo';
import { NavigationData } from '../../shared/types/navigation-data.type';
import { SelectedUnit } from '../../shared/types/selected-unit.type';
import { MissionService } from '../../services/mission.service';

describe('DisplayQuadrantComponent', () => {
  const unitServiceFake: UnitService = FakeClass.getInstance(UnitService);
  const missionServiceFake: MissionService = FakeClass.getInstance(MissionService);
  const loginSessionServiceFake: LoginSessionService = FakeClass.getInstance(LoginSessionService);
  const helper: CommonComponentTestHelper<DisplayQuadrantComponent> = new CommonComponentTestHelper(
    DisplayQuadrantComponent,
    {
      declarations: [
        PlanetDisplayNamePipe,
        ModalComponent
      ],
      providers: [
        { provide: UnitService, useValue: unitServiceFake },
        { provide: MissionService, useValue: missionServiceFake },
        { provide: LoginSessionService, useValue: loginSessionServiceFake }
      ],
      imports: [
        FormsModule
      ],
      schemas: [
        CUSTOM_ELEMENTS_SCHEMA
      ]
    },
    true,
    false
  );

  // tslint:disable-next-line:max-line-length
  const navigationData: NavigationData = JSON.parse('{"galaxies":[{"id":12,"name":"Vía láctea","sectors":50,"quadrants":50},{"id":13,"name":"Pegasus","sectors":50,"quadrants":50},{"id":16,"name":"Vía Bug","sectors":4,"quadrants":4}],"planets":[{"id":215581,"name":null,"sector":24,"quadrant":4,"planetNumber":1,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215582,"name":null,"sector":24,"quadrant":4,"planetNumber":2,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215583,"name":null,"sector":24,"quadrant":4,"planetNumber":3,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215584,"name":null,"sector":24,"quadrant":4,"planetNumber":4,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215585,"name":null,"sector":24,"quadrant":4,"planetNumber":5,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215586,"name":null,"sector":24,"quadrant":4,"planetNumber":6,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215587,"name":null,"sector":24,"quadrant":4,"planetNumber":7,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215588,"name":null,"sector":24,"quadrant":4,"planetNumber":8,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215589,"name":null,"sector":24,"quadrant":4,"planetNumber":9,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215590,"name":null,"sector":24,"quadrant":4,"planetNumber":10,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215591,"name":"VS24C4N11","sector":24,"quadrant":4,"planetNumber":11,"ownerId":1,"ownerName":"KevinGuancheDarias","richness":40,"home":true,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215592,"name":"VS24C4N12","sector":24,"quadrant":4,"planetNumber":12,"ownerId":null,"ownerName":null,"richness":50,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215593,"name":null,"sector":24,"quadrant":4,"planetNumber":13,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215594,"name":null,"sector":24,"quadrant":4,"planetNumber":14,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215595,"name":null,"sector":24,"quadrant":4,"planetNumber":15,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215596,"name":null,"sector":24,"quadrant":4,"planetNumber":16,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215597,"name":null,"sector":24,"quadrant":4,"planetNumber":17,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215598,"name":null,"sector":24,"quadrant":4,"planetNumber":18,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215599,"name":null,"sector":24,"quadrant":4,"planetNumber":19,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null},{"id":215600,"name":null,"sector":24,"quadrant":4,"planetNumber":20,"ownerId":null,"ownerName":null,"richness":null,"home":null,"galaxyId":12,"galaxyName":"Vía láctea","specialLocationId":null,"specialLocationName":null}]}');
  const obtainedUnits: ObtainedUnit[] = [
    {
      id: 1,
      count: 6,
      unit: {
        id: 74,
        name: 'Fake Guys',
        image: '00663c30803f8d28c3a8e858650db8d4.png'
      }
    },
    {
      id: 1,
      count: 8,
      unit: {
        id: 32,
        name: 'Fake Cloned Guys',
        image: '00663c30803f8d28c3a8e858650db8d4.png'
      }
    }
  ];

  helper.testItCreates();

  it('OnInit should get user selected planet', () => {
    expect(helper.component.myPlanet).toBeFalsy();
    const planet = new PlanetPojo();
    planet.id = 2;
    (<any>loginSessionServiceFake).findSelectedPlanet = new BehaviorSubject(planet);
    helper.component.ngOnInit();
    expect(helper.component.myPlanet).toBe(planet);
  });

  describe('After view init and data loaded', () => {
    const planet = new PlanetPojo();
    planet.id = 2;
    beforeEach(async done => {
      helper.component.navigationData = navigationData;
      (<any>loginSessionServiceFake).findSelectedPlanet = new BehaviorSubject(planet);
      await helper.reloadView();
      done();
    });

    it('Should display all planets', () => {
      helper.testHtmlNumberOfElements('.card', navigationData.planets.length);
    });

    it('should open the modal when clicking the target planet', async done => {
      const modal: ModalComponent = helper.findComponentInstanceByAngularId('missionModal').component;
      expect(helper.component.selectedPlanet).toBeFalsy();
      expect(helper.component.obtainedUnits).toBeFalsy();
      helper.spyOn(unitServiceFake, 'findInMyPlanet').and.returnValue(of(obtainedUnits));
      spyOn(modal, 'show').and.callThrough();

      await helper.triggerEventOnElement('.planet-actions img', 'click');
      await helper.doWait(101);
      expect(helper.component.selectedPlanet).toBe(navigationData.planets[0]);
      expect(unitServiceFake.findInMyPlanet).toHaveBeenCalledTimes(1);
      expect(unitServiceFake.findInMyPlanet).toHaveBeenCalledWith(helper.component.myPlanet.id);
      expect(helper.component.obtainedUnits).toBe(obtainedUnits);
      expect(modal.show).toHaveBeenCalledTimes(1);
      await helper.reloadView();
      done();
    });
    describe('When modal is open', () => {
      const selector = 'app-deployed-units-list';
      let modal: ComponentElement<ModalComponent>;
      beforeEach(async done => {
        modal = helper.findComponentInstanceByAngularId('missionModal');
        helper.spyOn(unitServiceFake, 'findInMyPlanet').and.returnValue(of(obtainedUnits));
        await helper.triggerEventOnElement('.planet-actions img', 'click');
        await helper.doWait(101);
        await helper.reloadView();
        done();
      });
      helper.itDirectivesForSelector(
        selector,
        {
          name: 'obtainedUnits',
          value: obtainedUnits
        },
        {
          name: 'selectable',
          value: true
        }
      );

      it(`button "Send mission" should be disabled, till a selection has been made in ${selector}`, async done => {
        helper.testHtmlElementPresent('button');
        const selectedUnits: SelectedUnit[] = [
          { id: 1, count: 2 },
          { id: 2, count: 4 }
        ];
        const button: HTMLButtonElement = modal.element.querySelector('button');
        expect(button.disabled).toBeTruthy();
        await helper.triggerEventOnElement(selector, 'selection', selectedUnits);
        await helper.reloadView();
        expect(button.disabled).toBeFalsy();
        done();
      });

      it('When clicking the button, should send the mission', async done => {
        const selectedUnits: SelectedUnit[] = [
          { id: 1, count: 2 },
          { id: 2, count: 4 }
        ];
        helper.spyOn(missionServiceFake, 'sendExploreMission').and.returnValue(of(null));
        await helper.triggerEventOnElement(selector, 'selection', selectedUnits);
        await helper.reloadView();
        await helper.triggerEventOnElement('app-modal button', 'click');
        expect(missionServiceFake.sendExploreMission).toHaveBeenCalledTimes(1);
        expect(missionServiceFake.sendExploreMission).toHaveBeenCalledWith(
          helper.component.myPlanet,
          helper.component.selectedPlanet,
          selectedUnits
        );
        done();
      });
    });
  });

});
