import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Observable } from 'rxjs/Observable';

import { ResourcesEnum } from '../shared-enum/resources-enum';
import { UserPojo } from '../shared-pojo/user.pojo';
import { AutoUpdatedResources } from '../class/auto-updated-resources';

/**
 * Thi service contains the logged in user resources <br />
 * It keeps the interface synced (adding resources each second) <br />
 *
 *
 * @author Kevin Guanche Darias
 */
@Injectable()
export class ResourceManagerService {
  public get currentPrimaryResource(): BehaviorSubject<number> {
    return this._currentPrimaryResourceFloor;
  }

  public get currentSecondaryResource(): BehaviorSubject<number> {
    return this._currentSecondaryResourceFloor;
  }

  public get currentEnergy(): Observable<number> {
    return this._currentEnergyFloor.asObservable();
  }

  public get currentMaxEnergy(): BehaviorSubject<number> {
    return this._currentMaxEnergyFloor;
  }

  private _intervalId: number;

  private _currentPrimaryResource: number;
  private _currentPrimaryResourcePerSecond: number;
  private _currentSecondaryResource: number;
  private _currentSecondaryResourcePerSecond: number;
  private _currentEnergy: number;
  private _currentMaxEnergy: number;
  private _lastDate: Date;

  private _currentPrimaryResourceFloor: BehaviorSubject<number> = new BehaviorSubject(0);
  private _currentSecondaryResourceFloor: BehaviorSubject<number> = new BehaviorSubject(0);
  private _currentEnergyFloor: BehaviorSubject<number> = new BehaviorSubject(0);
  private _currentMaxEnergyFloor: BehaviorSubject<number> = new BehaviorSubject(0);

  constructor() {

  }

  /**
   *
   * @param {UserPojo} userPojo
   * @author Kevin Guanche Darias
   */
  public startHandling(userPojo?: UserPojo) {
    this.stopHandling();
    this._lastDate = new Date();
    if (userPojo) {
      this._setResources(userPojo);
    }
    this._intervalId = window.setInterval(() => {
      const currentDate: Date = new Date();

      this._currentPrimaryResource = this._calculateSum(
        currentDate,
        this._lastDate,
        this._currentPrimaryResourcePerSecond,
        this._currentPrimaryResource
      );
      this._currentPrimaryResourceFloor.next(Math.floor(this._currentPrimaryResource));

      this._currentSecondaryResource = this._calculateSum(
        currentDate,
        this._lastDate,
        this._currentSecondaryResourcePerSecond,
        this._currentSecondaryResource
      );
      this._currentSecondaryResourceFloor.next(Math.floor(this._currentSecondaryResource));

      this._lastDate = currentDate;
    }, 1000);
  }

  public stopHandling() {
    window.clearInterval(this._intervalId);
    this._intervalId = null;
  }

  /**
   * Will add value to resource count
   *
   * @param {ResourcesEnum} resourceType Type of resource (primary,secondary ...)
   * @param {number} value Number to add
   *
   * @author Kevin Guanche Darias
   */
  public addResources(resourceType: ResourcesEnum, value: number): void {
    switch (resourceType) {
      case ResourcesEnum.PRIMARY:
        this._currentPrimaryResource += value;
        this._currentPrimaryResource = this._currentPrimaryResource < 0 ? 0 : this._currentPrimaryResource;
        break;
      case ResourcesEnum.SECONDARY:
        this._currentSecondaryResource += value;
        this._currentSecondaryResource = this._currentSecondaryResource < 0 ? 0 : this._currentSecondaryResource;
        break;
      case ResourcesEnum.CONSUMED_ENERGY:
        this._currentEnergy += value;
        this._currentEnergyFloor.next(Math.floor(this._currentEnergy));
        break;
      default:
        throw new Error('Unexpected type ' + resourceType);
    }
  }

  /**
   * Will substract value to resource count
   *
   * @param {ResourcesEnum} resourceType Type of resource (primary,secondary ...)
   * @param {number} value Number to substract
   *
   * @author Kevin Guanche Darias
   */
  public minusResources(resourceType: ResourcesEnum, value: number): void {
    this.addResources(resourceType, value * -1);
  }


  /**
   * Creates an AutoUpdatedResources instance <br>
   * Replaces the functionality of BaseHttpService.resourcesAutoUpdate()
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @param {boolean} [startAutoUpdate=true] If specified the instance will be synced with the user resources
   * @returns {AutoUpdatedResources}
   * @memberof ResourceManagerService
   */
  public createAutoUpdateResources(startAutoUpdate = true): AutoUpdatedResources {
    const retVal: AutoUpdatedResources = new AutoUpdatedResources(this);
    if (startAutoUpdate) {
      retVal.resourcesAutoUpdate();
    }
    return retVal;
  }

  private _setResources(userPojo: UserPojo) {
    this._setPrimaryValue(userPojo.primaryResource);
    this._setSecondaryValue(userPojo.secondaryResource);

    this._currentPrimaryResourcePerSecond = userPojo.computedPrimaryResourceGenerationPerSecond;
    this._currentSecondaryResourcePerSecond = userPojo.computedSecondaryResourceGenerationPerSecond;
    this._currentEnergy = userPojo.consumedEnergy;
    this._currentMaxEnergy = userPojo.maxEnergy;

    this._currentEnergyFloor.next(Math.floor(this._currentEnergy));
    this._currentMaxEnergyFloor.next(Math.floor(this._currentMaxEnergy));
  }

  private _setPrimaryValue(value: number): void {
    this._currentPrimaryResource = value;
    this._currentPrimaryResourceFloor.next(Math.floor(this._currentPrimaryResource));
  }

  private _setSecondaryValue(value: number): void {
    this._currentSecondaryResource = value;
    this._currentSecondaryResourceFloor.next(Math.floor(this._currentSecondaryResource));
  }

  /**
   * calculates the new value using the date diff and the value per second<br />
   * <b>WARNING!: To avoid float madness ensure that atleast there is a interval of 500ms</b>
   *
   * @param {Date} present datetime representing now
   * @param {Date} past datetime represents the last time value was update
   * @param {number} perSecondValue Value to increase each second
   * @param {number} value current Value
   * @returns {number} The new value for the given resource
   * @author Kevin Guanche Darias
   */
  private _calculateSum(present: Date, past: Date, perSecondValue: number, value: number): number {
    let retVal = value;
    const difference: number = (present.getTime() - past.getTime()) / 1000;

    retVal += (difference * perSecondValue);
    return retVal;
  }
}
