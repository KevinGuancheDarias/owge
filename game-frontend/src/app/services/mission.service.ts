import { Injectable } from '@angular/core';
import { GameBaseService } from '../service/game-base.service';
import { PlanetPojo } from '../shared-pojo/planet.pojo';
import { SelectedUnit } from '../shared/types/selected-unit.type';
import { Observable } from 'rxjs/Observable';
import { UnitMissionInformation } from '../shared/types/unit-mission-information.type';

@Injectable()
export class MissionService extends GameBaseService {

  public constructor() {
    super();
  }

  public sendExploreMission(sourcePlanet: PlanetPojo, targetPlanet: PlanetPojo, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._doPostWithAuthorizationToGame<UnitMissionInformation>(
      'mission/explorePlanet', {
        sourcePlanetId: sourcePlanet.id,
        targetPlanetId: targetPlanet.id,
        involvedUnits
      }
    );
  }
}
