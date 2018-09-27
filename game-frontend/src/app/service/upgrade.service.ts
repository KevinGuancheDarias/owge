import { Observable } from 'rxjs/Observable';
import { Injectable } from '@angular/core';
import { URLSearchParams } from '@angular/http';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

import { GameBaseService } from './game-base.service';
import { RunningUpgrade } from './../shared-pojo/running-upgrade.pojo';
import { ResourcesEnum } from '../shared-enum/resources-enum';
import { ResourceManagerService } from './resource-manager.service';
import { RequirementPojo } from './../shared-pojo/requirement.pojo';
import { ObtainedUpgradePojo } from './../shared-pojo/obtained-upgrade.pojo';

@Injectable()
export class UpgradeService extends GameBaseService {

  public get isUpgrading(): BehaviorSubject<RunningUpgrade> {
    return this._isUpgrading;
  }
  private _isUpgrading: BehaviorSubject<RunningUpgrade> = new BehaviorSubject(null);

  private _isUpgradingInternalData: RunningUpgrade;
  private _runningUpgradeCheckIntervalId: number;

  constructor(private _resourceManagerService: ResourceManagerService) {
    super();
    this.resourcesAutoUpdate();
  }

  public findObtained(): Observable<ObtainedUpgradePojo[]> {
    return this.doGetWithAuthorizationToGame('upgrade/findObtained');
  }

  /**
   * Checks in the backend if there is an upgrade mission going <br />
   * Will flush result to isUpgrading observable
   *
   * @todo In the future set next(null) on upgrade mission timeout
   * @author Kevin Guanche Darias
   */
  public backendRunningUpgradeCheck(): void {
    this.doGetWithAuthorizationToGame('upgrade/findRunningUpgrade').subscribe(res => {
      this._isUpgradingInternalData = res;
      if (res) {
        res.terminationDate = new Date(res.terminationDate);
        this._registerInterval();
      }
      this._isUpgrading.next(res);
    });
  }

  /**
   * Computes required resources by the next upgrade level
   *
   * @param {ObtainedUpgradePojo} ObtainedUpgradePojo
   *          - Notice: this function alters this object
   * @param {boolean} subscribeToResources true if want to recompute the runnable field of RequirementPojo,
   *          on each change to the resources (expensive!)
   * @returns obtainedUpgrade with filled values
   * @author Kevin Guanche Darias
   */
  public computeReqiredResources(obtainedUpgrade: ObtainedUpgradePojo, subscribeToResources = false): ObtainedUpgradePojo {
    const upgradeRef = obtainedUpgrade.upgrade;
    const requirements: RequirementPojo = new RequirementPojo();
    requirements.requiredPrimary = upgradeRef.primaryResource;
    requirements.requiredSecondary = upgradeRef.secondaryResource;
    requirements.requiredTime = upgradeRef.time;

    const nextLevel = obtainedUpgrade.level + 1;
    for (let i = 1; i < nextLevel; i++) {
      requirements.requiredPrimary += (requirements.requiredPrimary * upgradeRef.levelEffect);
      requirements.requiredSecondary += (requirements.requiredSecondary * upgradeRef.levelEffect);
      requirements.requiredTime += (requirements.requiredTime * upgradeRef.levelEffect);
    }
    if (subscribeToResources) {
      requirements.startDynamicRunnable(this._resourceManagerService);
    } else {
      requirements.checkRunnable(this.resources);
    }

    obtainedUpgrade.requirements = requirements;
    return obtainedUpgrade;
  }

  /**
   * Registers the upgrade in the server
   *
   * @author Kevin Guanche Darias
   */
  public registerLevelUp(obtainedUpgrade: ObtainedUpgradePojo): void {
    const params: URLSearchParams = new URLSearchParams();
    params.append('upgradeId', obtainedUpgrade.upgrade.id.toString());
    this.doGetWithAuthorizationToGame('upgrade/registerLevelUp', params).subscribe(res => {
      this._resourceManagerService.minusResources(ResourcesEnum.PRIMARY, obtainedUpgrade.requirements.requiredPrimary);
      this._resourceManagerService.minusResources(ResourcesEnum.SECONDARY, obtainedUpgrade.requirements.requiredSecondary);
      if (res) {
        res.terminationDate = new Date(res.terminationDate);
        this._registerInterval();
      }
      this._isUpgradingInternalData = res;
      this.isUpgrading.next(res);
    });
  }

  public cancelUpgrade(): void {
    this.doGetWithAuthorizationToGame('upgrade/cancelUpgrade').subscribe(() => {
      this._resourceManagerService.addResources(ResourcesEnum.PRIMARY, this._isUpgradingInternalData.requiredPrimary);
      this._resourceManagerService.addResources(ResourcesEnum.SECONDARY, this._isUpgradingInternalData.requiredSecondary);
      this._isUpgradingInternalData = null;
      this._isUpgrading.next(null);
    });
  }

  /**
   * Checks if upgrade termination date has end
   * If it has done will publish isUpgrading as null
   *
   * @author Kevin Guanche Darias
   */
  private _checkIsYetUpgrading(): void {
    let now: Date = new Date();
    now = new Date(now.getTime() - 1000);
    if (!this._isUpgradingInternalData || now >= this._isUpgradingInternalData.terminationDate) {
      this._clearInterval();
    }
  }

  private _registerInterval(): void {
    this._runningUpgradeCheckIntervalId = window.setInterval(() => this._checkIsYetUpgrading(), 1000);
  }

  /**
   * Clears the interval if required and sets isUpgrading as null
   *
   * @author Kevin Guanche Darias
   */
  private _clearInterval(): void {
    if (this._runningUpgradeCheckIntervalId) {
      clearInterval(this._runningUpgradeCheckIntervalId);
      this._runningUpgradeCheckIntervalId = null;
      this._isUpgrading.next(null);
    }
  }
}
