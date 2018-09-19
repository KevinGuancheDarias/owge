import { TestBed, inject } from '@angular/core/testing';

import { MissionService } from './mission.service';
import { CommonServiceTestHelper } from '../../helpers/common-service-test.helper';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestMetadataBuilder } from '../../helpers/test-metadata.builder';
import { PlanetPojo } from '../shared-pojo/planet.pojo';
import { SelectedUnit } from '../shared/types/selected-unit.type';
import { GameCommonTestHelper } from '../../helpers/game-common-test.helper';

describe('MissionService', () => {
  const helper: CommonServiceTestHelper<MissionService> = new CommonServiceTestHelper(
    MissionService,
    true,
    new TestMetadataBuilder().withDependency('BaseHttpService').getTestModuleMetadata());
  const sourcePlanet = new PlanetPojo();
  sourcePlanet.id = 3;
  const targetPlanet = new PlanetPojo();
  targetPlanet.id = 8;
  const selectedUnits: SelectedUnit[] = [
    { id: 2, count: 8 },
    { id: 74, count: 19 }
  ];
  const gameHelper = new GameCommonTestHelper(helper);
  let httpMock: HttpTestingController;
  beforeEach(() => httpMock = TestBed.get(HttpTestingController));
  helper.testItCreates();

  it('Explore mission should properly send the request', async done => {
    gameHelper.mockUniverse().mockGetHttpClientHeaders();
    helper.serviceInstance.sendExploreMission(sourcePlanet, targetPlanet, selectedUnits).toPromise();
    const req = httpMock.expectOne('/fakeverse/mission/explorePlanet');
    expect(req.request.method).toBe('POST');
    const body = req.request.body;
    expect(body.sourcePlanetId).toBe(sourcePlanet.id);
    expect(body.targetPlanetId).toBe(targetPlanet.id);
    expect(body.involvedUnits).toBe(selectedUnits);
    gameHelper.expectHttpClientHeaders(req.request.headers);
    req.flush('{}');
    done();
  });

  afterEach(() => httpMock.verify());
});
