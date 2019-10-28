import { TestBed } from '@angular/core/testing';

import { UnitType } from '@owge/universe';

import { UnitTypeService } from './unit-type.service';
import { CommonServiceTestHelper } from '../../helpers/common-service-test.helper';
import { TestMetadataBuilder } from '../../helpers/test-metadata.builder';
import { LoginSessionService } from '../login-session/login-session.service';
import { GameCommonTestHelper } from '../../helpers/game-common-test.helper';
import { PlanetPojo } from '../shared-pojo/planet.pojo';
import { UserPojo } from '../shared-pojo/user.pojo';

fdescribe('UnitTypeService', () => {
  const helper: CommonServiceTestHelper<UnitTypeService> = new CommonServiceTestHelper(
    UnitTypeService,
    false,
    new TestMetadataBuilder()
      .withDependency('BaseHttpService')
      .withAppendMockProviders([
        {
          provide: LoginSessionService,
          value: {
            genHttpHeaders: () => new Headers(),
            doGetWithAuthorizationToGame: () => { },
            findTokenData: () => { }
          }
        }
      ])
      .getTestModuleMetadata()
  );
  helper.configureTestingModule().configureServiceLocator();
  const gameHelper = new GameCommonTestHelper(helper);
  let unitTypes: UnitType[] = [
    {
      id: 1,
      image: '',
      name: 'Troops',
      canExplore: 'ANY',
      canGather: 'ANY',
      canConquest: 'ANY',
      canCounterattack: 'ANY',
      canDeploy: 'ANY',
      canEstablishBase: 'ANY',
      userBuilt: null
    }
  ];
  unitTypes = unitTypes.concat(
    { ...unitTypes[0], id: 2, name: 'Ships' },
    { ...unitTypes[0], id: 3, name: 'Defenses', canDeploy: 'OWNED_ONLY' },
    { ...unitTypes[0], id: 4, name: 'NoneGuys', canConquest: 'NONE' }
  );
  let behaviorLoaded: Promise<void>;
  beforeEach(() => {
    gameHelper.mockUniverse();
    const loginSessionService = TestBed.get(LoginSessionService);
    behaviorLoaded = new Promise(resolve => {
      // Please review git repository history for this file
      resolve();
    });
  });
  helper.createService();
  beforeEach(async done => {
    await behaviorLoaded;
    done();
  });
  helper.testItCreates();

  it('idsToUnitTypes should work as expected', async done => {
    const [defenses, ships, troops]: UnitType[] = await helper.serviceInstance.idsToUnitTypes(3, 2, 1);
    expect(defenses.name).toBe(unitTypes[2].name);
    expect(ships.name).toBe(unitTypes[1].name);
    expect(troops.name).toBe(unitTypes[0].name);
    done();
  });

  describe('canDoMission should properly detect id the mission is runnable or not ... When ', () => {
    const targetPlanet: PlanetPojo = new PlanetPojo();
    const ownerId = 1;
    const owner: UserPojo = new UserPojo();
    owner.id = 497;
    targetPlanet.ownerId = ownerId;
    beforeEach(() => gameHelper.mockUserTokenData(owner));

    it('status is ANY for everyone', () => {
      expect(helper.serviceInstance.canDoMission(targetPlanet, unitTypes, 'EXPLORE')).toBeTruthy();
    });
    it('status is NONE for at least one', () => {
      expect(helper.serviceInstance.canDoMission(targetPlanet, unitTypes, 'CONQUEST')).toBeFalsy();
    });
    it('status is OWNED_ONLY for at least one', () => {
      expect(helper.serviceInstance.canDoMission(targetPlanet, unitTypes, 'DEPLOY')).toBeFalsy();
      owner.id = ownerId;
      expect(helper.serviceInstance.canDoMission(targetPlanet, unitTypes, 'DEPLOY')).toBeTruthy();
    });
  });
});
