import { Injectable } from '@angular/core';
import { map, filter, tap, take } from 'rxjs/operators';
import { Observable } from 'rxjs';

import { ProgrammingError, LoadingService, UserStorage, User, DateUtil } from '@owge/core';
import { UniverseGameService, MissionStore, UnitRunningMission, RunningMission } from '@owge/universe';

import { PlanetPojo } from '../shared-pojo/planet.pojo';
import { SelectedUnit } from '../shared/types/selected-unit.type';
import { AnyRunningMission } from '../shared/types/any-running-mission.type';
import { MissionType } from '@owge/core';
import { AbstractWebsocketApplicationHandler } from '../interfaces/abstract-websocket-application-handler';

@Injectable()
export class MissionService extends AbstractWebsocketApplicationHandler {

  public constructor(
    private _universeGameService: UniverseGameService,
    private _loadingService: LoadingService,
    _userStore: UserStorage<User>,
    private _missionStore: MissionStore
  ) {
    super();
    this._eventsMap = {
      unit_mission_change: 'reacquireMissions',
      missions_count_change: '_onMissionsCountChange',
      enemy_mission_change: 'onEnemyMissionChange'
    };
    _userStore.currentUserImprovements.subscribe(improvement =>
      _missionStore.maxMissions.next(improvement.moreMisions)
    );
  }

  /**
   * Loads the count of missions in the <i>MissionStore</i>
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   */
  public loadCount(): Observable<number> {
    return this._universeGameService.requestWithAutorizationToContext<number>('game', 'get', 'mission/count')
      .pipe(tap(count => {
        this._missionStore.missionsCount.next(count);
      }));
  }

  public findMyRunningMissions(): Observable<UnitRunningMission[]> {
    return this._missionStore.myUnitMissions.asObservable();
  }

  public findEnemyRunningMissions(): Observable<UnitRunningMission[]> {
    return this._missionStore.enemyUnitMissions.asObservable();
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
    return this._universeGameService.postWithAuthorizationToUniverse(`mission/cancel?id=${missionId}`, {});
  }

  public isUnitMission(mission: RunningMission): boolean {
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

  /**
   * Reacts to WS unit_mission_change event
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param content
   */
  public reacquireMissions(content: { count: number, myUnitMissions: UnitRunningMission[] }): void {
    this._onMissionsCountChange(content.count);
    this._missionStore.myUnitMissions.next(content.myUnitMissions.map(mission => DateUtil.computeBrowserTerminationDate(mission)));
  }

  /**
   * Reacts to WS enemy_mission_change event
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param content
   */
  public onEnemyMissionChange(content: UnitRunningMission[]): void {
    this._missionStore.enemyUnitMissions.next(content.map(mission => DateUtil.computeBrowserTerminationDate(mission)));
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returns
   */
  public async workaroundSync(): Promise<void> {
    await this.loadCount().pipe(take(1)).toPromise();
    this._missionStore.myUnitMissions.next(
      await this._universeGameService.requestWithAutorizationToContext<UnitRunningMission[]>('game', 'get', 'mission/findMy')
        .pipe(
          take(1),
          map(obResult => obResult.map(current => DateUtil.computeBrowserTerminationDate(current)))
        ).toPromise()
    );
    this._missionStore.enemyUnitMissions.next(
      await this._universeGameService.requestWithAutorizationToContext<UnitRunningMission[]>('game', 'get', 'mission/findEnemy')
        .pipe(
          take(1),
          map(obResult => obResult.map(current => DateUtil.computeBrowserTerminationDate(current)))
        ).toPromise()
    );
  }

  protected _onMissionsCountChange(content: number) {
    this._missionStore.missionsCount.next(content);
  }

  private _sendMission(url: string, sourcePlanet: PlanetPojo, targetPlanet: PlanetPojo, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._universeGameService.postWithAuthorizationToUniverse<AnyRunningMission>(
      url, {
      sourcePlanetId: sourcePlanet.id,
      targetPlanetId: targetPlanet.id,
      involvedUnits
    }).pipe(map(result => {
      if (result) {
        this._missionStore.missionsCount.next(result.missionsCount);
      }
    }));
  }
}
