import { Injectable } from '@angular/core';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { camelCase, upperFirst } from 'lodash-es';


import { ProgrammingError, LoadingService, User, DateUtil, ResourcesEnum } from '@owge/core';
import {
  UniverseGameService, MissionStore, UnitRunningMission, RunningMission, UserStorage,
  UniverseCacheManagerService, WsEventCacheService, TypeWithMissionLimitation, Planet, MissionSupport, ResourceManagerService
} from '@owge/universe';

import { SelectedUnit } from '../shared/types/selected-unit.type';
import { AnyRunningMission } from '../shared/types/any-running-mission.type';
import { MissionType } from '@owge/core';
import { AbstractWebsocketApplicationHandler } from '@owge/core';

@Injectable()
export class MissionService extends AbstractWebsocketApplicationHandler {

  private _currentUser: User;

  public constructor(
    private _universeGameService: UniverseGameService,
    private _loadingService: LoadingService,
    userStore: UserStorage<User>,
    private _missionStore: MissionStore,
    private _universeCacheManagerService: UniverseCacheManagerService,
    private _wsEventCacheService: WsEventCacheService,
    private _resourceManagerService: ResourceManagerService
  ) {
    super();
    this._eventsMap = {
      unit_mission_change: '_onMyUnitMissionsChange',
      missions_count_change: '_onMissionsCountChange',
      enemy_mission_change: '_onEnemyMissionChange',
      mission_gather_result: '_onMissionGatherResult'
    };
    userStore.currentUser.subscribe(user => this._currentUser = user);
    userStore.currentUserImprovements.subscribe(improvement =>
      _missionStore.maxMissions.next(improvement.moreMisions)
    );
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
    await this._wsEventCacheService.updateWithFrontendComputedData('unit_mission_change', content);
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
    await this._wsEventCacheService.updateWithFrontendComputedData('enemy_mission_change', content);
  }


  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @protected
   * @param content
   */
  protected _onMissionGatherResult(content: { primaryResource: number, secondaryResource: number }): void {
    this._resourceManagerService.addResources(ResourcesEnum.PRIMARY, content.primaryResource || 0);
    this._resourceManagerService.addResources(ResourcesEnum.SECONDARY, content.secondaryResource || 0);
  }

  protected async _onMissionsCountChange(content: number) {
    this._missionStore.missionsCount.next(content);
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
