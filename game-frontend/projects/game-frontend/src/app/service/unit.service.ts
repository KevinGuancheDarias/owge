import { HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  AbstractWebsocketApplicationHandler,
  DateUtil,
  LoggerHelper,
  OrderUtil,
  ProgrammingError,
} from '@owge/core';
import { Improvement, User } from '@owge/types/core';
import { PlanetService } from '@owge/galaxy';
import {
  AutoUpdatedResources, Planet, ResourceManagerService, ResourceRequirements,
  UnitStore, UnitUtil, UniverseGameService, UserStorage, WsEventCacheService
} from '@owge/universe';
import {
  ObtainedUnit, ObtainedUpgrade, PlanetsUnitsRepresentation, Unit, UnitBuildRunningMission,
  UnitUpgradeRequirements, SelectedUnit
} from '@owge/types/universe';
import { isEqual } from 'lodash-es';
import { Observable, Subject, Subscription } from 'rxjs';
import { distinctUntilChanged, map, tap } from 'rxjs/operators';
import { ConfigurationService } from '../modules/configuration/services/configuration.service';
import { CriticalAttackInformation } from '../types/critical-attack-information.type';
import { UpgradeService } from './upgrade.service';

@Injectable()
export class UnitService extends AbstractWebsocketApplicationHandler {
  private static readonly _LOG: LoggerHelper = new LoggerHelper(UnitService.name);

  private _selectedPlanet: Planet;
  private _resources: AutoUpdatedResources;
  private _improvement: Improvement;
  private _unitStore: UnitStore = new UnitStore;
  private _onUnlockedChangeSubscription: Subscription;

  constructor(
    private _resourceManagerService: ResourceManagerService,
    private _universeGameService: UniverseGameService,
    private _userStore: UserStorage<User>,
    private _planetService: PlanetService,
    private _upgradeService: UpgradeService,
    private _wsEventCacheService: WsEventCacheService,
    private _configurationService: ConfigurationService
  ) {
    super();
    this._eventsMap = {
      // eslint-disable-next-line @typescript-eslint/naming-convention
      unit_unlocked_change: '_onUnlockedChange',
      // eslint-disable-next-line @typescript-eslint/naming-convention
      unit_obtained_change: '_onObtainedChange',
      // eslint-disable-next-line @typescript-eslint/naming-convention
      unit_build_mission_change: '_onBuildMissionChange'
    };
    this._userStore.currentUserImprovements.pipe(distinctUntilChanged(isEqual)).subscribe(improvement => this._improvement = improvement);
    this._resources = new AutoUpdatedResources(_resourceManagerService);
    this._planetService.findCurrentPlanet().subscribe(currentSelected => this._selectedPlanet = currentSelected);
  }

  /**
   * Computes required resources
   *
   * @param unit
   *          - Notice: this function alters this object
   * @param subscribeToResources true if want to recompute the runnable field of RequirementPojo,
   *          on each change to the resources (expensive!)
   * @param countBehabiorSubject - Specify it to automatically update resource requirements on changes to count
   * @param defineSubscription Used to allow external unsubscription
   * @returns Unit with filled values
   * @author Kevin Guanche Darias
   */
  public computeRequiredResources(
      unit: Unit,
      subscribeToResources: boolean,
      countBehaviorSubject: Subject<number>,
      defineSubscription: (subscription: Subscription, improvementSubscription: Subscription) => void
  ): Unit {
    let improvementSuscription: Subscription;
    const subscription = countBehaviorSubject.pipe(distinctUntilChanged((a, b) => a === b)).subscribe(newCount => {
      if (improvementSuscription) {
        improvementSuscription.unsubscribe();
      }
      improvementSuscription = this._userStore.currentUserImprovements.pipe(distinctUntilChanged(isEqual)).subscribe(improvement => {
        if (unit.requirements) {
          unit.requirements.stopDynamicRunnable();
        }
        const improvementStep = this._configurationService.findParamOrDefault('IMPROVEMENT_STEP', 10).value;
        unit.requirements = new ResourceRequirements();
        unit.requirements.requiredPrimary = unit.primaryResource * newCount;
        unit.requirements.requiredSecondary = unit.secondaryResource * newCount;
        unit.requirements.requiredTime = Math.ceil(unit.requirements.computeImprovementValue(
          unit.time * newCount,
          improvement.moreUnitBuildSpeed,
          improvementStep,
          false
        ));
        unit.requirements.requiredEnergy = (unit.energy || 0) * newCount;
        this._doCheckResourcesSubscriptionForRequirements(unit.requirements, subscribeToResources);
      });
      defineSubscription(null, improvementSuscription);
    });
    defineSubscription(subscription, null);
    return unit;
  }

  /**
   * Will register a unit build mission <b>for selected planet</b>
   * <b>NOTICE:</b> If success auto updates the user resources based on requirements
   *
   * @param unit to be build
   * @param count  of that unit
   * @author Kevin Guanche Darias
   */
  public registerUnitBuild(unit: Unit, count: number): void {
    let params: HttpParams = new HttpParams();
    params = params.append('planetId', this._selectedPlanet.id.toString());
    params = params.append('unitId', unit.id.toString());
    params = params.append('count', count.toString());
    this._universeGameService.postWithAuthorizationToUniverse<UnitBuildRunningMission>('unit/build', '', { params }).subscribe(res => {
      if (res) {
        DateUtil.computeLocalTerminationDate(res);
      }
    });
  }

  /**
   * Cancels a unit build mission
   *
   * @param missionData
   * @memberof UnitService
   * @author Kevin Guanche Darias
   */
  public cancel(missionData: UnitBuildRunningMission): Observable<void> {
    let params: HttpParams = new HttpParams();
    params = params.append('missionId', missionData.missionId);
    return this._universeGameService.getWithAuthorizationToUniverse('unit/cancel', { params });
  }

  /**
   * Finds unit in selected planet <br>
   * <b>NOTICE:</b> Backend should throw if you do not own the planet<br>
   * <b>Returns a copy as unit alterarion may affect the distinctUntilChanged</b>
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param planetId
   * @returns
   * @memberof UnitService
   */
  public findInMyPlanet(planetId: number): Observable<ObtainedUnit[]> {
    (window as any).exposedIsEqual = isEqual;
    return this._unitStore.obtained.pipe(
      map(content => content.planets[planetId] || []),
      distinctUntilChanged((a, b) => isEqual(a, b))
    );
  }

  /**
   * Finds the building mission for the given planet, if any
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param planetId
   * @returns
   */
  public findBuildingMissionInMyPlanet(planetId: number): Observable<UnitBuildRunningMission> {
    return this._unitStore.runningBuildMissions.pipe(
      map(content => content.find(current => current.sourcePlanet.id === planetId) || null)
    );
  }

  /**
   * Deletes specified obtainedUnit, if count is exactly the totally available, will completely remove the obtainedUnit
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param unit Should contain at least the id, and the count to delete
   * @param count
   * @returns
   * @throws {ProgrammingError} Invalid unit was passed
   * @memberof UnitService
   */
  public async deleteObtainedUnit(unit: ObtainedUnit, count: number): Promise<void> {
    if (!unit.id || !count) {
      throw new ProgrammingError('ObtainedUnit MUST have an id, and the count MUST be specified');
    }
    const { id } = unit;
    await this._universeGameService.requestWithAutorizationToContext('game', 'post', 'unit/delete', { id, count })
      .toPromise();
  }

  public obtainedUnitToSelectedUnits(obtainedUnits: ObtainedUnit[]): SelectedUnit[] {
    return obtainedUnits.map(current => ({
      unit: current.unit,
      count: current.count,
      expirationId: current.temporalInformation?.id
    }));
  }

  /**
   * Find unit upgrade requirements for given logged user faction
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @returns
   * @memberof UnitService
   */
  public findUnitUpgradeRequirements(): Observable<UnitUpgradeRequirements[]> {
    return this._unitStore.upgradeRequirements.pipe(
      tap(result => result.forEach(current => {
        current.allReached = current.allReached = current.requirements.every(requirement => requirement.reached);
      }))
    );
  }

  /**
   * Returns the list of unlocked units!
   *
   * @author Kevin Guanche Darias
   */
  public findUnlocked(): Observable<Unit[]> {
    return this._unitStore.unlocked.asObservable();
  }

  public findCriticalAttackInformation(unit: Unit): Observable<CriticalAttackInformation[]> {
    return this._universeGameService.requestWithAutorizationToContext('game', 'get', `unit/${unit.id}/criticalAttack`);
  }

  protected async _onUnlockedChange(content: Unit[]): Promise<void> {
    const unitUpgradeRequirements: UnitUpgradeRequirements[] = await this._wsEventCacheService.findStoredValue('unit_requirements_change');
    if (this._onUnlockedChangeSubscription) {
      this._onUnlockedChangeSubscription.unsubscribe();
      delete this._onUnlockedChangeSubscription;
    }
    this._onUnlockedChangeSubscription = this._upgradeService.findObtained().subscribe(upgrades => {
      unitUpgradeRequirements.forEach(current => this._computeRequirementsReached(current, upgrades));
      this._unitStore.upgradeRequirements.next(unitUpgradeRequirements);
    });
    const sorted = OrderUtil.doOrder(content);
    this._unitStore.unlocked.next(sorted);
  }

  protected async _onObtainedChange(content: ObtainedUnit[]): Promise<void> {
    this._unitStore.obtained.next(
      this._createPlanetsRepresentation(content, (unit) => unit.sourcePlanet.id)
    );
  }

  protected async _onBuildMissionChange(content: UnitBuildRunningMission[]): Promise<void> {
    content.forEach(current => DateUtil.computeBrowserTerminationDate(current));
    this._unitStore.runningBuildMissions.next(content);
    await this._wsEventCacheService.updateWithFrontendComputedData('unit_build_mission_change', content);
  }

  private _doCheckResourcesSubscriptionForRequirements(requirements: ResourceRequirements, subscribeToResources: boolean) {
    if (subscribeToResources) {
      requirements.startDynamicRunnable(this._resourceManagerService);
    } else {
      requirements.checkRunnable(this._resources);
    }
  }

  private _createPlanetsRepresentation(
    units: ObtainedUnit[],
    keyGetter: (unit: ObtainedUnit) => any
  ): PlanetsUnitsRepresentation<ObtainedUnit[]> {

    const unitsMap: Map<string, ObtainedUnit[]> = new Map();
    units.sort((a, b) => OrderUtil.compareForSort(a.unit, b.unit)).forEach(unit => {
      UnitUtil.createTerminationDate(unit);
      const planetId: string = keyGetter(unit);
      const collection: ObtainedUnit[] = unitsMap.get(planetId);
      if (!collection) {
        unitsMap.set(planetId, [unit]);
      } else {
        collection.push(unit);
      }
    });
    const planetUnitsRepresentation: PlanetsUnitsRepresentation<ObtainedUnit[]> = { planets: {} } as any;
    unitsMap.forEach((value, key) => planetUnitsRepresentation.planets[key] = value);
    return planetUnitsRepresentation;
  }

  private _computeRequirementsReached(unitRequirement: UnitUpgradeRequirements, obtainedUpgrades: ObtainedUpgrade[]): void {
    unitRequirement.requirements.forEach(currentRequirement => {
      currentRequirement.reached = obtainedUpgrades.some(
        upgrade => upgrade.upgrade.id === currentRequirement.upgrade.id && upgrade.level >= currentRequirement.level
      );
    });
  }
}
