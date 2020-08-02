import { Injectable } from '@angular/core';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { camelCase, upperFirst } from 'lodash-es';


import { ProgrammingError, LoadingService, UserStorage, User, DateUtil, StorageOfflineHelper } from '@owge/core';
import {
  UniverseGameService, MissionStore, UnitRunningMission, RunningMission,
  UniverseCacheManagerService, WsEventCacheService, TypeWithMissionLimitation, Planet, MissionSupport
} from '@owge/universe';

import { SelectedUnit } from '../shared/types/selected-unit.type';
import { AnyRunningMission } from '../shared/types/any-running-mission.type';
import { MissionType } from '@owge/core';
import { AbstractWebsocketApplicationHandler } from '@owge/core';

@Injectable()
export class MissionService extends AbstractWebsocketApplicationHandler {

  private _currentUser: User;

  private _offlineMyUnitMissionsStore: StorageOfflineHelper<UnitRunningMission[]>;
  private _offlineEnemyUnitMissionsStore: StorageOfflineHelper<UnitRunningMission[]>;
  private _offlineCountUnitMissionsStore: StorageOfflineHelper<number>;

  public constructor(
    private _universeGameService: UniverseGameService,
    private _loadingService: LoadingService,
    userStore: UserStorage<User>,
    private _missionStore: MissionStore,
    private _universeCacheManagerService: UniverseCacheManagerService,
    private _wsEventCacheService: WsEventCacheService
  ) {
    super();
    this._eventsMap = {
      unit_mission_change: '_onMyUnitMissionsChange',
      missions_count_change: '_onMissionsCountChange',
      enemy_mission_change: '_onEnemyMissionChange'
    };
    userStore.currentUser.subscribe(user => this._currentUser = user);
    userStore.currentUserImprovements.subscribe(improvement =>
      _missionStore.maxMissions.next(improvement.moreMisions)
    );
  }

  public async createStores(): Promise<void> {
    this._offlineMyUnitMissionsStore = this._universeCacheManagerService.getStore('mission.my');
    this._offlineEnemyUnitMissionsStore = this._universeCacheManagerService.getStore('mission.enemy');
    this._offlineCountUnitMissionsStore = this._universeCacheManagerService.getStore('mission.count');
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returns
   */
  public async workaroundSync(): Promise<void> {
    const count: number = await this._wsEventCacheService.findFromCacheOrRun(
      'missions_count_change',
      this._offlineCountUnitMissionsStore,
      async () => await this._universeGameService.requestWithAutorizationToContext('game', 'get', 'mission/count').toPromise()
    );
    this._onMyUnitMissionsChange({
      count,
      myUnitMissions: await this._wsEventCacheService.findFromCacheOrRun(
        'unit_mission_change',
        this._offlineMyUnitMissionsStore,
        () => this._universeGameService.requestWithAutorizationToContext<UnitRunningMission[]>('game', 'get', 'mission/findMy').pipe(
          map(obResult => obResult.map(current => DateUtil.computeBrowserTerminationDate(current)))
        ).toPromise()
      )
    });
    this._onMissionsCountChange(count);
    this._onEnemyMissionChange(
      await this._wsEventCacheService.findFromCacheOrRun('enemy_mission_change', this._offlineEnemyUnitMissionsStore,
        async () =>
          await this._universeGameService.requestWithAutorizationToContext<UnitRunningMission[]>('game', 'get', 'mission/findEnemy')
            .pipe(
              map(obResult => obResult.map(current => DateUtil.computeBrowserTerminationDate(current)))
            ).toPromise()
      )
    );
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returns
   */
  public async workaroundInitialOffline(): Promise<void> {
    const count: number = await this._offlineCountUnitMissionsStore.find();
    if (typeof count === 'number') {
      await this._offlineMyUnitMissionsStore.doIfNotNull(content => this._onMyUnitMissionsChange({ count, myUnitMissions: content }));
    }
    await this._offlineEnemyUnitMissionsStore.doIfNotNull(content => this._onEnemyMissionChange(content));
  }


  /**
   * Check if the specified type with mission limitation can do the mission
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param planet
   * @param typeWithMissionLimitation
   * @param missionType
   * @returns
   */
  public canDoMission(planet: Planet, typeWithMissionLimitation: TypeWithMissionLimitation[], missionType: MissionType): boolean {
    return typeWithMissionLimitation.filter(current => current).every(current => {
      const status: MissionSupport = current[`can${upperFirst(camelCase(missionType))}`];
      switch (status) {
        case 'ANY':
          return true;
        case 'NONE':
          return false;
        case 'OWNED_ONLY':
          return planet.ownerId === (this._currentUser && this._currentUser.id);
        default:
          throw new ProgrammingError(`Unsupported MissionSupport ${status}`);
      }
    });
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
   * @param {Planet} sourcePlanet
   * @param {Planet} targetPlanet
   * @param {SelectedUnit[]} selectedUnits
   * @returns {Promise<void>}
   * @memberof MissionService
   */
  public async sendMission(
    missionType: MissionType,
    sourcePlanet: Planet,
    targetPlanet: Planet,
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

  public sendExploreMission(sourcePlanet: Planet, targetPlanet: Planet, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._sendMission('mission/explorePlanet', sourcePlanet, targetPlanet, involvedUnits);
  }

  public sendGatherMission(sourcePlanet: Planet, targetPlanet: Planet, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._sendMission('mission/gather', sourcePlanet, targetPlanet, involvedUnits);
  }

  public sendEstablishBaseMission(sourcePlanet: Planet, targetPlanet: Planet, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._sendMission('mission/establishBase', sourcePlanet, targetPlanet, involvedUnits);
  }

  public sendAttackMission(sourcePlanet: Planet, targetPlanet: Planet, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._sendMission('mission/attack', sourcePlanet, targetPlanet, involvedUnits);
  }

  public sendCounterattackMission(sourcePlanet: Planet, targetPlanet: Planet, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._sendMission('mission/counterattack', sourcePlanet, targetPlanet, involvedUnits);
  }

  public sendConquestMission(sourcePlanet: Planet, targetPlanet: Planet, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._sendMission('mission/conquest', sourcePlanet, targetPlanet, involvedUnits);
  }

  public sendDeploy(sourcePlanet: Planet, targetPlanet: Planet, involvedUnits: SelectedUnit[]): Observable<void> {
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
  protected async _onMyUnitMissionsChange(content: { count: number, myUnitMissions: UnitRunningMission[] }): Promise<void> {
    this._onMissionsCountChange(content.count);
    const withBrowserDateContent: UnitRunningMission[] = content.myUnitMissions
      .map(mission => DateUtil.computeBrowserTerminationDate(mission));
    this._missionStore.myUnitMissions.next(withBrowserDateContent);
    await this._offlineMyUnitMissionsStore.save(withBrowserDateContent);
  }

  /**
   * Reacts to WS enemy_mission_change event
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param content
   */
  protected async _onEnemyMissionChange(content: UnitRunningMission[]): Promise<void> {
    const withBrowserDateContent: UnitRunningMission[] = content.map(mission => DateUtil.computeBrowserTerminationDate(mission));
    this._missionStore.enemyUnitMissions.next(withBrowserDateContent);
    await this._offlineEnemyUnitMissionsStore.save(withBrowserDateContent);
  }

  protected async _onMissionsCountChange(content: number) {
    this._missionStore.missionsCount.next(content);
    await this._offlineCountUnitMissionsStore.save(content);
  }

  private _sendMission(url: string, sourcePlanet: Planet, targetPlanet: Planet, involvedUnits: SelectedUnit[]): Observable<void> {
    return this._universeGameService.postWithAuthorizationToUniverse<AnyRunningMission>(
      url, {
      sourcePlanetId: sourcePlanet.id,
      targetPlanetId: targetPlanet.id,
      involvedUnits: involvedUnits.map(involvedUnit => ({ id: involvedUnit.unit.id, count: involvedUnit.count }))
    }).pipe(map(result => {
      if (result) {
        this._missionStore.missionsCount.next(result.missionsCount);
      }
    }));
  }
}
