import { Injectable } from '@angular/core';
import { tap } from 'rxjs/operators/tap';
import { Observable } from 'rxjs/Observable';

import { GameBaseService } from '../service/game-base.service';
import { PlanetPojo } from '../shared-pojo/planet.pojo';
import { SelectedUnit } from '../shared/types/selected-unit.type';
import { UnitMissionInformation } from '../shared/types/unit-mission-information.type';
import { AnyRunningMission } from '../shared/types/any-running-mission.type';
import { UnitRunningMission } from '../shared/types/unit-running-mission.type';
import { MissionType } from '../shared/types/mission.type';
import { ProgrammingError } from '../../error/programming.error';
import { ClockSyncService } from '../modules/core/services/clock-sync.service';

@Injectable()
export class MissionService extends GameBaseService {

  public constructor(private _clockSyncService: ClockSyncService) {
    super();
  }

  public findMyRunningMissions(): Observable<AnyRunningMission[]> {
    return this._syncDate(this.doGetWithAuthorizationToGame<AnyRunningMission[]>('mission/findMy'));
  }

  public findEnemyRunningMissions(): Observable<UnitRunningMission[]> {
    return this._syncDate(this.doGetWithAuthorizationToGame<AnyRunningMission[]>('mission/findEnemy'));
  }

  /**
   * Sends a mission whose type is specified by param
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param {MissionType} missionType
   * @param {PlanetPojo} sourcePlanet
   * @param {PlanetPojo} targetPlanet
   * @param {SelectedUnit[]} selectedUnits
   * @returns {Promise<void>}
   * @memberof MissionService
   */
  public async sendMission(
    missionType: MissionType,
    sourcePlanet: PlanetPojo,
    targetPlanet: PlanetPojo,
    selectedUnits: SelectedUnit[]
  ): Promise<void> {
    await this._loadingService.runWithLoading(async () => {
      switch (missionType) {
        case 'EXPLORE':
          await this.sendExploreMission(sourcePlanet, targetPlanet, selectedUnits).toPromise();
          break;
        case 'GATHER':
          await this.sendGatherMission(sourcePlanet, targetPlanet, selectedUnits).toPromise();
          break;
        case 'ESTABLISH_BASE':
          await this.sendEstablishBaseMission(sourcePlanet, targetPlanet, selectedUnits).toPromise();
          break;
        case 'ATTACK':
          await this.sendAttackMission(sourcePlanet, targetPlanet, selectedUnits).toPromise();
          break;
        case 'COUNTERATTACK':
          await this.sendCounterattackMission(sourcePlanet, targetPlanet, selectedUnits).toPromise();
          break;
        case 'CONQUEST':
          await this.sendConquestMission(sourcePlanet, targetPlanet, selectedUnits).toPromise();
          break;
        case 'DEPLOY':
          await this.sendDeploy(sourcePlanet, targetPlanet, selectedUnits).toPromise();
          break;
        default:
          throw new ProgrammingError(`Unexpected mission type ${missionType}`);
      }
    });
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

  public sendDeploy(sourcePlanet: PlanetPojo, targetPlanet: PlanetPojo, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._sendMission('mission/deploy', sourcePlanet, targetPlanet, involvedUnits);
  }

  public cancelMission(missionId: number): Observable<void> {
    return this._doPostWithAuthorizationToGame(`mission/cancel?id=${missionId}`, {});
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
      case 'DEPLOY':
      case 'DEPLOYED':
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

  private _syncDate(input: Observable<AnyRunningMission[]>): Observable<AnyRunningMission[]> {
    return input.pipe(
      tap(current =>
        current.forEach(
          runningMission =>
            runningMission.terminationDate = this._clockSyncService.computeSyncedTerminationDate(runningMission.terminationDate)
        )
      )
    );
  }
}
