import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Improvement, DateUtil } from '@owge/core';
import {
  UniverseGameService, UpgradeStore, ObtainedUpgrade, UpgradeRunningMission,
  AutoUpdatedResources, ResourceManagerService, ResourceRequirements
} from '@owge/universe';

import { AbstractWebsocketApplicationHandler } from '@owge/core';
import { map } from 'rxjs/operators';

@Injectable()
export class UpgradeService extends AbstractWebsocketApplicationHandler {

  private _upgradeStore: UpgradeStore = new UpgradeStore;
  private _resources: AutoUpdatedResources;

  constructor(
    private _resourceManagerService: ResourceManagerService,
    private _universeGameService: UniverseGameService
  ) {
    super();
    this._resources = new AutoUpdatedResources(_resourceManagerService);
    this._eventsMap = {
      obtained_upgrades_change: '_onObtainedChange',
      running_upgrade_change: '_onRunningChange'
    };
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returns
   */
  public async workaroundSync(): Promise<void> {
    this._upgradeStore.obtained.next(
      await this._universeGameService.requestWithAutorizationToContext('game', 'get', 'upgrade/findObtained').toPromise()
    );
    this._upgradeStore.runningLevelUpMission.next(
      await this._universeGameService.requestWithAutorizationToContext<UpgradeRunningMission>('game', 'get', 'upgrade/findRunningUpgrade')
        .pipe(
          map(result => DateUtil.computeBrowserTerminationDate(result))
        ).toPromise()
    );
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
   * @param {ObtainedUpgrade} ObtainedUpgrade
   *          - Notice: this function alters this object
   * @param {boolean} subscribeToResources true if want to recompute the runnable field of RequirementPojo,
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
  public registerLevelUp(obtainedUpgrade: ObtainedUpgrade): void {
    let params: HttpParams = new HttpParams();
    params = params.append('upgradeId', obtainedUpgrade.upgrade.id.toString());
    this._universeGameService.getWithAuthorizationToUniverse('upgrade/registerLevelUp', { params }).subscribe(res => {

    });
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

  protected _onObtainedChange(content: ObtainedUpgrade[]): void {
    this._upgradeStore.obtained.next(content);
  }

  protected _onRunningChange(content: UpgradeRunningMission): void {
    this._upgradeStore.runningLevelUpMission.next(DateUtil.computeBrowserTerminationDate(content));
  }
}
