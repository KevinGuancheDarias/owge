import { Injectable } from '@angular/core';
import {
  AbstractWebsocketApplicationHandler, DateUtil, LoadingService, MissionSupport,
  MissionType, ProgrammingError, ResourcesEnum, TypeWithMissionLimitation, User
} from '@owge/core';

import { camelCase, upperFirst } from 'lodash-es';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Planet } from '../pojos/planet.pojo';
import { MissionStore } from '../storages/mission.store';
import { UserStorage } from '../storages/user.storage';
import { AnyRunningMission } from '../types/any-running-mission.type';
import { RunningMission } from '../types/running-mission.type';
import { SelectedUnit } from '../types/selected-unit.type';
import { UnitRunningMission } from '../types/unit-running-mission.type';
import { UnitUtil } from '../utils/unit.util';
import { ResourceManagerService } from './resource-manager.service';
import { UniverseGameService } from './universe-game.service';
import { WsEventCacheService } from './ws-event-cache.service';

@Injectable()
export class MissionService extends AbstractWebsocketApplicationHandler {

  private _currentUser: User;

  public constructor(
    private _universeGameService: UniverseGameService,
    private _loadingService: LoadingService,
    userStore: UserStorage<User>,
    private _missionStore: MissionStore,
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
      _missionStore.maxMissions.next(improvement.moreMissions)
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

  public findMyRunningMissions<U extends User = User>(): Observable<UnitRunningMission<U>[]> {
    return this._missionStore.myUnitMissions.asObservable();
  }

  public findEnemyRunningMissions<U extends User = User>(): Observable<UnitRunningMission<U>[]> {
    return this._missionStore.enemyUnitMissions.asObservable();
  }

  /**
   * Sends a mission whose type is specified by param
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.0
   * @param missionType
   * @param sourcePlanet
   * @param targetPlanet
   * @param selectedUnits
   * @returns
   * @memberof MissionService
   */
  public async sendMission(
    missionType: MissionType,
    sourcePlanet: Planet,
    targetPlanet: Planet,
    selectedUnits: SelectedUnit[],
    wantedTime?: number
  ): Promise<void> {
    await this._loadingService.runWithLoading(async () => {
      switch (missionType) {
        case 'EXPLORE':
          await this.sendExploreMission(sourcePlanet, targetPlanet, selectedUnits, wantedTime).toPromise();
          break;

        case 'GATHER':
          await this.sendGatherMission(sourcePlanet, targetPlanet, selectedUnits, wantedTime).toPromise();
          break;

        case 'ESTABLISH_BASE':
          await this.sendEstablishBaseMission(sourcePlanet, targetPlanet, selectedUnits, wantedTime).toPromise();
          break;

        case 'ATTACK':
          await this.sendAttackMission(sourcePlanet, targetPlanet, selectedUnits, wantedTime).toPromise();
          break;

        case 'COUNTERATTACK':
          await this.sendCounterattackMission(sourcePlanet, targetPlanet, selectedUnits, wantedTime).toPromise();
          break;

        case 'CONQUEST':
          await this.sendConquestMission(sourcePlanet, targetPlanet, selectedUnits, wantedTime).toPromise();
          break;

        case 'DEPLOY':
          await this.sendDeploy(sourcePlanet, targetPlanet, selectedUnits, wantedTime).toPromise();
          break;
        default:
          throw new ProgrammingError(`Unexpected mission type ${missionType}`);
      }
    });
  }

  public sendExploreMission(
    sourcePlanet: Planet,
    targetPlanet: Planet,
    involvedUnits: SelectedUnit[],
    wantedTime?: number
  ): Observable<void> {
    return this._sendMission('mission/explorePlanet', sourcePlanet, targetPlanet, involvedUnits, wantedTime);
  }

  public sendGatherMission(
    sourcePlanet: Planet,
    targetPlanet: Planet,
    involvedUnits: SelectedUnit[],
    wantedTime?: number
  ): Observable<void> {
    return this._sendMission('mission/gather', sourcePlanet, targetPlanet, involvedUnits, wantedTime);
  }

  public sendEstablishBaseMission(
    sourcePlanet: Planet,
    targetPlanet: Planet,
    involvedUnits: SelectedUnit[],
    wantedTime?: number
  ): Observable<void> {
    return this._sendMission('mission/establishBase', sourcePlanet, targetPlanet, involvedUnits, wantedTime);
  }

  public sendAttackMission(
    sourcePlanet: Planet,
    targetPlanet: Planet,
    involvedUnits: SelectedUnit[],
    wantedTime?: number
  ): Observable<void> {
    return this._sendMission('mission/attack', sourcePlanet, targetPlanet, involvedUnits, wantedTime);
  }

  public sendCounterattackMission(
    sourcePlanet: Planet,
    targetPlanet: Planet,
    involvedUnits: SelectedUnit[],
    wantedTime?: number
  ): Observable<void> {
    return this._sendMission('mission/counterattack', sourcePlanet, targetPlanet, involvedUnits, wantedTime);
  }

  public sendConquestMission(
    sourcePlanet: Planet,
    targetPlanet: Planet,
    involvedUnits: SelectedUnit[],
    wantedTime?: number
  ): Observable<void> {
    return this._sendMission('mission/conquest', sourcePlanet, targetPlanet, involvedUnits, wantedTime);
  }

  public sendDeploy(sourcePlanet: Planet, targetPlanet: Planet, involvedUnits: SelectedUnit[], wantedTime?: number): Observable<void> {
    return this._sendMission('mission/deploy', sourcePlanet, targetPlanet, involvedUnits, wantedTime);
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
  protected async _onMyUnitMissionsChange(content: { count: number; myUnitMissions: UnitRunningMission[] }): Promise<void> {
    this._onMissionsCountChange(content.count);
    content.myUnitMissions.forEach(missions => missions.involvedUnits.forEach(ou => UnitUtil.createTerminationDate(ou)));
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
  protected _onMissionGatherResult(content: { primaryResource: number; secondaryResource: number }): void {
    this._resourceManagerService.addResources(ResourcesEnum.PRIMARY, content.primaryResource || 0);
    this._resourceManagerService.addResources(ResourcesEnum.SECONDARY, content.secondaryResource || 0);
  }

  protected async _onMissionsCountChange(content: number) {
    this._missionStore.missionsCount.next(content);
  }

  private _sendMission(
    url: string,
    sourcePlanet: Planet,
    targetPlanet: Planet,
    involvedUnits: SelectedUnit[],
    wantedTime?: number
  ): Observable<void> {
    return this._universeGameService.postWithAuthorizationToUniverse<AnyRunningMission>(
      url, {
      sourcePlanetId: sourcePlanet.id,
      targetPlanetId: targetPlanet.id,
      involvedUnits: involvedUnits.map(unit => this.mapSelectedUnitToBackendExpected(unit)),
      wantedTime
    }).pipe(map(result => {
      if (result) {
        this._missionStore.missionsCount.next(result.missionsCount);
      }
    }));
  }

  private mapSelectedUnitToBackendExpected(unit: SelectedUnit): SelectedUnit {
    return {
      id: unit.unit.id,
      count: unit.count,
      expirationId: unit.expirationId,
      storedUnits: unit.storedUnits?.map(storedUnit => this.mapSelectedUnitToBackendExpected(storedUnit))
    };
  }
}
