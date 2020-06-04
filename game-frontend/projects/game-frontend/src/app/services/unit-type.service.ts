
import { Injectable } from '@angular/core';
import { Observable, BehaviorSubject } from 'rxjs';
import { camelCase, upperFirst } from 'lodash-es';

import { ProgrammingError, Improvement, AbstractWebsocketApplicationHandler, StorageOfflineHelper } from '@owge/core';
import { UniverseGameService, UnitType, UnitTypeStore, UniverseCacheManagerService, WsEventCacheService } from '@owge/universe';

import { MissionType } from '@owge/core';
import { MissionSupport } from '../../../../owge-universe/src/lib/types/mission-support.type';
import { LoginSessionService } from '../login-session/login-session.service';
import { Planet } from '@owge/galaxy';

@Injectable()
export class UnitTypeService extends AbstractWebsocketApplicationHandler {

  private _oldCount = 0;
  private _unitTypeStore: UnitTypeStore = new UnitTypeStore;
  private _currentValue: UnitType[];
  private _offlineUnitTypes: StorageOfflineHelper<UnitType[]>;

  public constructor(
    private _loginSessionService: LoginSessionService,
    private _universeGameService: UniverseGameService,
    private _wsEventCacheService: WsEventCacheService,
    universeCacheManagerService: UniverseCacheManagerService
  ) {
    super();
    this._eventsMap = {
      unit_type_change: '_onUnitTypeChange'
    };
    this._offlineUnitTypes = universeCacheManagerService.getStore('unit_type.values');
  }

  public async workaroundSync(): Promise<void> {
    this._onUnitTypeChange(await this._wsEventCacheService.findFromCacheOrRun('unit_type_change', this._offlineUnitTypes, async () =>
      await this._universeGameService.requestWithAutorizationToContext('game', 'get', 'unitType/').toPromise()
    ));
  }

  public async workaroundInitialOffline(): Promise<void> {
    this._offlineUnitTypes.doIfNotNull(content => this._onUnitTypeChange(content));
  }

  public getUnitTypes(): Observable<UnitType[]> {
    return this._unitTypeStore.userValues.asObservable();
  }

  /**
   * Returns the available number that the user can build of a given unit type
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {number} id unit type id
   * @returns {number}
   * @throws {ProgrammingError} If type doesn't exists
   * @memberof UnitTypeService
   */
  public findAvailableByType(id: number): number {
    const type: UnitType = this._findTypeById(id);
    return type.computedMaxCount - type.userBuilt;
  }

  /**
   * Returns true, if user has enough avaiable for the given unit type
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {number} typeId
   * @param {number} requiredCount Count wanted to be added by the user
   * @returns {boolean}
   * @memberof UnitTypeService
   */
  public hasAvailable(typeId: number, requiredCount: number): boolean {
    const type: UnitType = this._findTypeById(typeId);
    return !type.maxCount || this.findAvailableByType(typeId) >= requiredCount;
  }

  /**
   * Test if all the unitTypes passed can do the specified mission
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {PlanetPojo} planet
   * @param {UnitType[]} unitTypes
   * @param {MissionType} missionType
   * @returns {boolean}
   * @memberof UnitTypeService
   */
  public canDoMission(planet: Planet, unitTypes: UnitType[], missionType: MissionType): boolean {
    return unitTypes.every(current => {
      const status: MissionSupport = current[`can${upperFirst(camelCase(missionType))}`];
      switch (status) {
        case 'ANY':
          return true;
        case 'NONE':
          return false;
        case 'OWNED_ONLY':
          return planet.ownerId === this._loginSessionService.findTokenData().id;
        default:
          throw new ProgrammingError(`Unsupported MissionSupport ${status}`);
      }
    });
  }

  /**
   * Converts an array of ids to an array of unitTypes
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {...number[]} ids
   * @returns {Promise<UnitType[]>}
   * @memberof UnitTypeService
   */
  public idsToUnitTypes(...ids: number[]): Promise<UnitType[]> {
    return new Promise(resolve => {
      this.getUnitTypes().subscribe(result => {
        resolve(ids.map(currentId => result.find(currentUnitType => currentUnitType.id === currentId)));
      });
    });
  }

  protected _onUnitTypeChange(content: UnitType[]): void {
    content.map(current => {
      if (!current.userBuilt) {
        current.userBuilt = 0;
      }
      return current;
    });
    this._currentValue = content;
    this._unitTypeStore.userValues.next(content);
    this._offlineUnitTypes.save(content);
  }

  private _findTypeById(id: number): UnitType {
    const retVal: UnitType = this._currentValue.find(current => current.id === id);
    if (!retVal) {
      throw new ProgrammingError(`No UnitType with id ${id} was found`);
    }
    return retVal;
  }
}
