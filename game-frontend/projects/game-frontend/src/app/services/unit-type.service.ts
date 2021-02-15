
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ProgrammingError, AbstractWebsocketApplicationHandler, StorageOfflineHelper, UnitType } from '@owge/core';
import {
  UniverseGameService, Planet, UnitTypeStore, UniverseCacheManagerService,
  WsEventCacheService
} from '@owge/universe';

import { MissionType } from '@owge/core';
import { MissionService } from './mission.service';

@Injectable()
export class UnitTypeService extends AbstractWebsocketApplicationHandler {

  private _unitTypeStore: UnitTypeStore = new UnitTypeStore;
  private _currentValue: UnitType[];
  private _offlineUnitTypes: StorageOfflineHelper<UnitType[]>;

  public constructor(
    private _universeGameService: UniverseGameService,
    private _wsEventCacheService: WsEventCacheService,
    private _universeCacheManagerService: UniverseCacheManagerService,
    private _missionService: MissionService
  ) {
    super();
    this._eventsMap = {
      unit_type_change: '_onUnitTypeChange'
    };
  }

  public async createStores(): Promise<void> {
    this._offlineUnitTypes = this._universeCacheManagerService.getStore('unit_type.values');
  }

  public async workaroundSync(): Promise<void> {
    this._onUnitTypeChange(await this._wsEventCacheService.findFromCacheOrRun('unit_type_change', this._offlineUnitTypes, async () =>
      await this._universeGameService.requestWithAutorizationToContext('game', 'get', 'unitType/').toPromise()
    ));
  }

  public async workaroundInitialOffline(): Promise<void> {
    await this._offlineUnitTypes.doIfNotNull(content => this._onUnitTypeChange(content));
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
    const targetType = this._findShareMaxCountRoot(type);
    return !targetType.maxCount || this.findAvailableByType(targetType.id) >= requiredCount;
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
    return this._missionService.canDoMission(planet, unitTypes, missionType);
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

  protected async _onUnitTypeChange(content: UnitType[]): Promise<void> {
    content.map(current => {
      if (!current.userBuilt) {
        current.userBuilt = 0;
      }
      return current;
    });
    this._currentValue = content;
    this._unitTypeStore.userValues.next(content);
    await this._offlineUnitTypes.save(content);
  }

  private _findTypeById(id: number): UnitType {
    const retVal: UnitType = this._currentValue.find(current => current.id === id);
    if (!retVal) {
      throw new ProgrammingError(`No UnitType with id ${id} was found`);
    }
    return retVal;
  }

  private _findShareMaxCountRoot(unitType: UnitType): UnitType {
    return unitType.shareMaxCount ? unitType.shareMaxCount : unitType;
  }
}
