import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';

import { ClockSyncService, UniverseGameService } from '@owge/universe';

import { RunningUpgrade } from './../shared-pojo/running-upgrade.pojo';
import { ResourcesEnum } from '../shared-enum/resources-enum';
import { ResourceManagerService } from './resource-manager.service';
import { RequirementPojo } from './../shared-pojo/requirement.pojo';
import { ObtainedUpgradePojo } from './../shared-pojo/obtained-upgrade.pojo';
import { AutoUpdatedResources } from '../class/auto-updated-resources';
import { Improvement } from '@owge/core/owge-core';

@Injectable()
export class UpgradeService {

  public get isUpgrading(): Observable<RunningUpgrade> {
    return this._isUpgrading.asObservable();
  }
  private _isUpgrading: BehaviorSubject<RunningUpgrade> = new BehaviorSubject(null);

  private _isUpgradingInternalData: RunningUpgrade;
  private _runningUpgradeCheckIntervalId: number;

  private _resources: AutoUpdatedResources;

  constructor(
    private _resourceManagerService: ResourceManagerService,
    private _clockSyncService: ClockSyncService,
    private _universeGameService: UniverseGameService
  ) {
    this._resources = new AutoUpdatedResources(_resourceManagerService);
  }

  public findObtained(): Observable<ObtainedUpgradePojo[]> {
    return this._universeGameService.getWithAuthorizationToUniverse('upgrade/findObtained');
  }


  /**
   * Finds obe obained upgrade by upgrade id
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @param upgradeId
   * @returns
   */
  public findOneObtained(upgradeId: number): Observable<ObtainedUpgradePojo> {
    return this._universeGameService.requestWithAutorizationToContext('game', 'get', `upgrade/findObtained/${upgradeId}`);
  }

  /**
   * Checks in the backend if there is an upgrade mission going <br />
   * Will flush result to isUpgrading observable
   *
   * @todo In the future set next(null) on upgrade mission timeout
   * @author Kevin Guanche Darias
   */
  public backendRunningUpgradeCheck(): void {
    this._universeGameService.getWithAuthorizationToUniverse('upgrade/findRunningUpgrade').subscribe(res => {
      if (res && (!this._isUpgradingInternalData || this._isUpgradingInternalData.missionId !== res.missionId)) {
        this._isUpgradingInternalData = res;
        res.terminationDate = this._clockSyncService.computeSyncedTerminationDate(res.terminationDate);
        this._registerInterval();
        this._isUpgrading.next(res);
      }
    });
  }

  /**
   * Computes required resources by the next upgrade level
   *
   * @param {ObtainedUpgradePojo} ObtainedUpgradePojo
   *          - Notice: this function alters this object
   * @param {boolean} subscribeToResources true if want to recompute the runnable field of RequirementPojo,
   *          on each change to the resources (expensive!)
   * @param [userImprovement] The improvement to apply, for example to calculate the time
   * @returns obtainedUpgrade with filled values
   * @author Kevin Guanche Darias
   */
  public computeReqiredResources(
    obtainedUpgrade: ObtainedUpgradePojo,
    subscribeToResources = false,
    userImprovement?: Improvement
  ): ObtainedUpgradePojo {
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
    if (userImprovement && userImprovement.moreUpgradeResearchSpeed) {
      requirements.requiredTime = requirements.handleSustractionPercentage(
        requirements.requiredTime,
        userImprovement.moreUpgradeResearchSpeed
      );
    }
    if (subscribeToResources) {
      requirements.startDynamicRunnable(this._resourceManagerService);
    } else {
      requirements.checkRunnable(this._resources);
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
    let params: HttpParams = new HttpParams();
    params = params.append('upgradeId', obtainedUpgrade.upgrade.id.toString());
    this._universeGameService.getWithAuthorizationToUniverse('upgrade/registerLevelUp', { params }).subscribe(res => {
      this._resourceManagerService.minusResources(ResourcesEnum.PRIMARY, obtainedUpgrade.requirements.requiredPrimary);
      this._resourceManagerService.minusResources(ResourcesEnum.SECONDARY, obtainedUpgrade.requirements.requiredSecondary);
      if (res) {
        res.terminationDate = new Date(res.terminationDate);
        this._registerInterval();
      }
      this._isUpgradingInternalData = res;
      this._isUpgrading.next(res);
    });
  }

  public cancelUpgrade(): void {
    this._universeGameService.getWithAuthorizationToUniverse('upgrade/cancelUpgrade').subscribe(() => {
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
    if (!this._runningUpgradeCheckIntervalId) {
      this._runningUpgradeCheckIntervalId = window.setInterval(() => this._checkIsYetUpgrading(), 1000);
    }
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
      delete this._isUpgradingInternalData;
      this._isUpgrading.next(null);
    }
  }
}
