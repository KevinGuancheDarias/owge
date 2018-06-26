import { Injectable } from '@angular/core';
import { GameBaseService } from '../service/game-base.service';
import { PlanetPojo } from '../shared-pojo/planet.pojo';
import { SelectedUnit } from '../shared/types/selected-unit.type';
import { Observable } from 'rxjs/Observable';
import { UnitMissionInformation } from '../shared/types/unit-mission-information.type';
import { AnyRunningMission } from '../shared/types/any-running-mission.type';
import { UnitRunningMission } from '../shared/types/unit-running-mission.type';
import { MissionType } from '../shared/types/mission.type';

@Injectable()
export class MissionService extends GameBaseService {

  public constructor() {
    super();
  }

  public findMyRunningMissions(): Observable<AnyRunningMission[]> {
    return this.doGetWithAuthorizationToGame<AnyRunningMission[]>('mission/findMy');
  }

  public findEnemyRunningMissions(): Observable<UnitRunningMission[]> {
    return this.doGetWithAuthorizationToGame<UnitRunningMission[]>('mission/findEnemy');
  }

  public sendExploreMission(sourcePlanet: PlanetPojo, targetPlanet: PlanetPojo, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._sendMission('mission/explorePlanet', sourcePlanet, targetPlanet, involvedUnits);
  }

  public sendGatherMission(sourcePlanet: PlanetPojo, targetPlanet: PlanetPojo, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._sendMission('mission/gather', sourcePlanet, targetPlanet, involvedUnits);
  }

  public sendEstablishBaseMission(sourcePlanet: PlanetPojo, targetPlanet: PlanetPojo, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._sendMission('mission/establishBase', sourcePlanet, targetPlanet, involvedUnits);
  }

  public sendAttackMission(sourcePlanet: PlanetPojo, targetPlanet: PlanetPojo, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._sendMission('mission/attack', sourcePlanet, targetPlanet, involvedUnits);
  }

  public sendCounterattackMission(sourcePlanet: PlanetPojo, targetPlanet: PlanetPojo, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._sendMission('mission/counterattack', sourcePlanet, targetPlanet, involvedUnits);
  }

  public sendConquestMission(sourcePlanet: PlanetPojo, targetPlanet: PlanetPojo, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._sendMission('mission/conquest', sourcePlanet, targetPlanet, involvedUnits);
  }

  public isUnitMission(mission: AnyRunningMission): boolean {
    switch (mission.type) {
      case 'RETURN_MISSION':
      case 'EXPLORE':
      case 'GATHER':
      case 'ESTABLISH_BASE':
      case 'ATTACK':
      case 'COUNTERATTACK':
      case 'CONQUEST':
        return true;
      default:
        return false;
    }
  }

  public isBuildMission(mission: AnyRunningMission): boolean {
    return mission.type === 'BUILD_UNIT';
  }

  private _sendMission(url: string, sourcePlanet: PlanetPojo, targetPlanet: PlanetPojo, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._doPostWithAuthorizationToGame<UnitMissionInformation>(
      url, {
        sourcePlanetId: sourcePlanet.id,
        targetPlanetId: targetPlanet.id,
        involvedUnits
      }
    );
  }
}
