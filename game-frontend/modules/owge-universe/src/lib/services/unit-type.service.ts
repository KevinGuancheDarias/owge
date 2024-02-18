
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {ProgrammingError, AbstractWebsocketApplicationHandler, StorageOfflineHelper} from '@owge/core';
import { UnitType, AttackRule, AttackRuleEntry, MissionType } from '@owge/types/core';

import { MissionService } from './mission.service';
import { UnitTypeStore } from '../storages/unit-type.store';
import { UniverseCacheManagerService } from './universe-cache-manager.service';
import { Planet } from '../pojos/planet.pojo';
import {map, take} from 'rxjs/operators';

@Injectable()
export class UnitTypeService extends AbstractWebsocketApplicationHandler {

  private _unitTypeStore: UnitTypeStore = new UnitTypeStore;
  private _currentValue: UnitType[];
  private _offlineUnitTypes: StorageOfflineHelper<UnitType[]>;

  public constructor(
    private _universeCacheManagerService: UniverseCacheManagerService,
    private _missionService: MissionService
  ) {
    super();
    this._eventsMap = {
      // eslint-disable-next-line @typescript-eslint/naming-convention
      unit_type_change: '_onUnitTypeChange'
    };
  }

  public async createStores(): Promise<void> {
    this._offlineUnitTypes = this._universeCacheManagerService.getStore('unit_type.values');
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
   * @param id unit type id
   * @returns
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
   * @param typeId
   * @param requiredCount Count wanted to be added by the user
   * @returns
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
   * @param planet
   * @param unitTypes
   * @param missionType
   * @returns
   * @memberof UnitTypeService
   */
  public canDoMission(planet: Planet, unitTypes: UnitType[], missionType: MissionType): boolean {
    return this._missionService.canDoMission(planet, unitTypes, missionType);
  }

  /**
   * Converts an array of ids to an array of unitTypes
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param ids
   * @returns
   * @memberof UnitTypeService
   */
  public idsToUnitTypes(...ids: number[]): Promise<UnitType[]> {
    return this.getUnitTypes().pipe(
        take(1),
        map(result => ids.map(currentId => result.find(currentUnitType => currentUnitType.id === currentId)))
      ).toPromise();
  }

  public idToUnitType(id: number): Promise<UnitType> {
    return this.getUnitTypes().pipe(
        take(1),
        map(unitTypes => unitTypes.find(unitType => unitType.id === id))
    ).toPromise();
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.20
   */
  public canAttack(attackRule: AttackRule, target: UnitType): boolean {
    if (!attackRule || !attackRule.entries || !attackRule.entries.length) {
      return true;
    } else {
      const appliedRule: AttackRuleEntry = attackRule.entries
        .find(entry => entry.target === 'UNIT_TYPE' && this._findUnitTypeMatchingRule(entry, target));

      return appliedRule ? appliedRule.canAttack : true;
    }
  }

  public isSameUnitTypeOrChild(wantedId: number, unitType: UnitType, unitTypeList: UnitType[] ): boolean {
    if(wantedId === unitType.id) {
      return true;
    } else if(unitType.parent){
      return this.isSameUnitTypeOrChild(wantedId, unitType.parent, unitTypeList);
    } else {
      return false;
    }
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.20
   * @param unitType
   * @returns
   */
  public findAppliedAttackRule(unitType: UnitType): AttackRule {
    if (unitType.attackRule) {
      return unitType.attackRule;
    } else if (unitType.parent) {
      return this.findAppliedAttackRule(unitType.parent);
    } else {
      return null;
    }
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

  private _findUnitTypeMatchingRule(entry: AttackRuleEntry, unitType: UnitType): UnitType {
    if (entry.referenceId === unitType.id) {
      return unitType;
    } else if (unitType.parent) {
      return this._findUnitTypeMatchingRule(entry, unitType.parent);
    } else {
      return null;
    }
  }
}
