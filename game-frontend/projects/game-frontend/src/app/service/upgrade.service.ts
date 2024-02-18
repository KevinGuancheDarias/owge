import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {DateUtil, OrderUtil, AbstractWebsocketApplicationHandler} from '@owge/core';
import {
  UniverseGameService, UpgradeStore,
  AutoUpdatedResources, ResourceManagerService, ResourceRequirements, WsEventCacheService
} from '@owge/universe';
import { ObtainedUpgrade, UpgradeRunningMission } from '@owge/types/universe';

import { Improvement } from '@owge/types/core';
import { map } from 'rxjs/operators';
import { ConfigurationService } from '../modules/configuration/services/configuration.service';

@Injectable()
export class UpgradeService extends AbstractWebsocketApplicationHandler {

  private _upgradeStore: UpgradeStore = new UpgradeStore;
  private _resources: AutoUpdatedResources;

  constructor(
    private _resourceManagerService: ResourceManagerService,
    private _universeGameService: UniverseGameService,
    private _wsEventCacheService: WsEventCacheService,
    private _configurationService: ConfigurationService
  ) {
    super();
    this._resources = new AutoUpdatedResources(_resourceManagerService);
    this._eventsMap = {
      obtained_upgrades_change: '_onObtainedChange',
      running_upgrade_change: '_onRunningChange'
    };
  }

  public findObtained(): Observable<ObtainedUpgrade[]> {
    return this._upgradeStore.obtained.asObservable();
  }

  /**
   * Finds obtained upgrade by upgrade id
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @param upgradeId
   * @returns
   */
  public findOneObtained(upgradeId: number): Observable<ObtainedUpgrade> {
    return this._upgradeStore.obtained.pipe(
      map(obtaineds => obtaineds.find(current => current.upgrade.id === upgradeId))
    );
  }

  /**
   * Computes required resources by the next upgrade level
   *
   * @param ObtainedUpgrade
   *          - Notice: this function alters this object
   * @param subscribeToResources true if want to recompute the runnable field of RequirementPojo,
   *          on each change to the resources (expensive!)
   * @param [userImprovement] The improvement to apply, for example to calculate the time
   * @returns obtainedUpgrade with filled values
   * @author Kevin Guanche Darias
   */
  public computeReqiredResources(
    obtainedUpgrade: ObtainedUpgrade,
    subscribeToResources = false,
    userImprovement?: Improvement
  ): ObtainedUpgrade {
    const upgradeRef = obtainedUpgrade.upgrade;
    const requirements: ResourceRequirements = new ResourceRequirements;
    requirements.requiredPrimary = upgradeRef.primaryResource;
    requirements.requiredSecondary = upgradeRef.secondaryResource;
    requirements.requiredTime = upgradeRef.time;

    const nextLevel = obtainedUpgrade.level + 1;
    const improvementStep = this._configurationService.findParamOrDefault('IMPROVEMENT_STEP', 10).value;
    for (let i = 1; i < nextLevel; i++) {
      requirements.requiredPrimary += (requirements.requiredPrimary * upgradeRef.levelEffect);
      requirements.requiredSecondary += (requirements.requiredSecondary * upgradeRef.levelEffect);
      requirements.requiredTime += (requirements.requiredTime * upgradeRef.levelEffect);
    }
    if (userImprovement && userImprovement.moreUpgradeResearchSpeed) {
      requirements.requiredTime = requirements.computeImprovementValue(
        requirements.requiredTime,
        userImprovement.moreUpgradeResearchSpeed,
        improvementStep,
        false
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
  public registerLevelUp(obtainedUpgrade: ObtainedUpgrade): Observable<void> {
    let params: HttpParams = new HttpParams();
    params = params.append('upgradeId', obtainedUpgrade.upgrade.id.toString());
    return this._universeGameService.getWithAuthorizationToUniverse('upgrade/registerLevelUp', { params });
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returns
   */
  public findRunningLevelUp(): Observable<UpgradeRunningMission> {
    return this._upgradeStore.runningLevelUpMission.asObservable();
  }

  public cancelUpgrade(): Promise<void> {
    return this._universeGameService.requestWithAutorizationToContext('game', 'get', 'upgrade/cancelUpgrade').toPromise();
  }

  protected async _onObtainedChange(content: ObtainedUpgrade[]): Promise<void> {
    this._upgradeStore.obtained.next(
        content.sort((a,b) => OrderUtil.compareForSort(a.upgrade, b.upgrade))
    );
  }

  protected async _onRunningChange(content: UpgradeRunningMission): Promise<void> {
    if (content) {
      this._upgradeStore.runningLevelUpMission.next(DateUtil.computeBrowserTerminationDate(content));
    } else {
      this._upgradeStore.runningLevelUpMission.next(null);
    }
    await this._wsEventCacheService.updateWithFrontendComputedData('running_upgrade_change', content);
  }
}
