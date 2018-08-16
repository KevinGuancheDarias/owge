import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

import { GameBaseService } from '../service/game-base.service';
import { UnitType } from '../shared/types/unit-type.type';
import { ProgrammingError } from '../../error/programming.error';

@Injectable()
export class UnitTypeService extends GameBaseService {

  private _unitTypes: BehaviorSubject<UnitType[]> = new BehaviorSubject(null);

  public constructor() {
    super();
    this.loadTypes();
  }

  public getUnitTypes(): Observable<UnitType[]> {
    return this._unitTypes.asObservable().filter(value => value !== null);
  }

  public loadTypes() {
    this.doGetWithAuthorizationToGame<UnitType[]>('unitType/').subscribe(result => {
      this._unitTypes.next(result.map(current => {
        if (!current.userBuilt) {
          current.userBuilt = 0;
        }
        return current;
      }));
    });
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
   * Adds to userbuilt count of the specified type <br>
   * <b>NOTICE:</b> Will trigger a new value to observers
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {number} id
   * @param {number} count
   * @throws {ProgrammingError} If type doesn't exists
   * @memberof UnitTypeService
   */
  public addToType(id: number, count: number): void {
    const type: UnitType = this._findTypeById(id);
    type.userBuilt += count;
    this._unitTypes.next(this._unitTypes.value);
  }

  /**
   * Sustract to userbuilt count of the specified type <br>
   * <b>NOTICE:</b> Will trigger a new value to observers
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {number} id
   * @param {number} count
   * @throws {ProgrammingError} If type doesn't exists
   * @memberof UnitTypeService
   */
  public sustractToType(id: number, count: number): void {
    this.addToType(id, -count);
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

  private _findTypeById(id: number): UnitType {
    const retVal: UnitType = this._unitTypes.value.find(current => current.id === id);
    if (!retVal) {
      throw new ProgrammingError(`No UnitType with id ${id} was found`);
    }
    return retVal;
  }
}
