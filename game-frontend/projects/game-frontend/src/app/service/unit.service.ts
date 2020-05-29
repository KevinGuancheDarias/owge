import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { take, map, tap } from 'rxjs/operators';
import { Observable, Subscription, Subject } from 'rxjs';

import {
  ProgrammingError, UserStorage, User, Improvement, LoggerHelper, DateUtil, AbstractWebsocketApplicationHandler
} from '@owge/core';
import {
  UniverseGameService, Unit, ResourceRequirements, ResourceManagerService, AutoUpdatedResources,
  UnitStore, ObtainedUnit, UnitBuildRunningMission, PlanetsUnitsRepresentation
} from '@owge/universe';
import { Planet, PlanetService } from '@owge/galaxy';

import { UnitUpgradeRequirements } from '../../../../owge-universe/src/lib/types/unit-upgrade-requirements.type';
import { SelectedUnit } from '../shared/types/selected-unit.type';

@Injectable()
export class UnitService extends AbstractWebsocketApplicationHandler {
  private static readonly _LOG: LoggerHelper = new LoggerHelper(UnitService.name);

  private _selectedPlanet: Planet;
  private _resources: AutoUpdatedResources;
  private _improvement: Improvement;
  private _unitStore: UnitStore = new UnitStore;

  constructor(
    private _resourceManagerService: ResourceManagerService,
    private _universeGameService: UniverseGameService,
    private _userStore: UserStorage<User>,
    private _planetService: PlanetService
  ) {
    super();
    this._eventsMap = {
      unit_unlocked_change: '_onUnlockedChange',
      unit_obtained_change: '_onObtainedChange',
      unit_build_mission_change: '_onBuildMissionChange'
    };
    this._userStore.currentUserImprovements.pipe(take(1)).subscribe(improvement => this._improvement = improvement);
    this._resources = new AutoUpdatedResources(_resourceManagerService);
    this._planetService.findCurrentPlanet().subscribe(currentSelected => this._selectedPlanet = currentSelected);
  }

  /**
   * Workarounds the syncing of unit related stuff
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returns
   */
  public async workaroundSync(): Promise<void> {
    this._onUnlockedChange(
      await this._universeGameService.requestWithAutorizationToContext('game', 'get', 'unit/findUnlocked')
        .pipe(take(1)).toPromise()
    );
    this._onObtainedChange(
      await this._universeGameService.requestWithAutorizationToContext('game', 'get', 'unit/find-in-my-planets')
        .pipe(take(1)).toPromise()
    );
    this._onBuildMissionChange(
      await this._universeGameService.requestWithAutorizationToContext('game', 'get', 'unit/build-missions')
        .pipe(take(1)).toPromise()
    );
  }

  /**
   * Computes required resources
   *
   * @param unit
   *          - Notice: this function alters this object
   * @param subscribeToResources true if want to recompute the runnable field of RequirementPojo,
   *          on each change to the resources (expensive!)
   * @param countBehabiorSubject - Specify it to automatically update resource requirements on changes to count
   * @returns Unit with filled values
   * @author Kevin Guanche Darias
   */
  public computeRequiredResources(unit: Unit, subscribeToResources: boolean, countBehaviorSubject: Subject<number>): Unit {
    if (!countBehaviorSubject) {
      this._doComputeRequiredResources(unit, subscribeToResources);
    } else {
      let improvementSuscription: Subscription;
      countBehaviorSubject.subscribe(newCount => {
        if (improvementSuscription) {
          improvementSuscription.unsubscribe();
        }
        improvementSuscription = this._userStore.currentUserImprovements.subscribe(improvement => {
          if (unit.requirements) {
            unit.requirements.stopDynamicRunnable();
          }
          unit.requirements = new ResourceRequirements();
          unit.requirements.requiredPrimary = unit.primaryResource * newCount;
          unit.requirements.requiredSecondary = unit.secondaryResource * newCount;
          unit.requirements.requiredTime = Math.ceil(unit.requirements.handleSustractionPercentage(
            unit.time * newCount,
            improvement.moreUnitBuildSpeed
          ));
          unit.requirements.requiredEnergy = (unit.energy || 0) * newCount;
          this._doCheckResourcesSubscriptionForRequirements(unit.requirements, subscribeToResources);
        });
      });
    }
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
    unit = this._doComputeRequiredResources(unit, false, count);
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
   * @param {RunningUnitPojo} missionData
   * @memberof UnitService
   * @author Kevin Guanche Darias
   */
  public cancel(missionData: UnitBuildRunningMission) {
    let params: HttpParams = new HttpParams();
    params = params.append('missionId', missionData.missionId);
    this._universeGameService.getWithAuthorizationToUniverse('unit/cancel', { params }).subscribe(() => {
    });
  }

  /**
   * Finds unit in selected planet <br>
   * <b>NOTICE:</b> Backend should throw if you do not own the planet
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {number} planetId
   * @returns {Observable<ObtainedUnit[]>}
   * @memberof UnitService
   */
  public findInMyPlanet(planetId: number): Observable<ObtainedUnit[]> {
    return this._unitStore.obtained.pipe(
      map(content => content.planets[planetId] || [])
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
   * @param {ObtainedUnit} unit Should contain at least the id, and the count to delete
   * @param count
   * @returns {Promise<void>}
   * @throws {ProgrammingError} Invalid unit was passed
   * @memberof UnitService
   */
  public async deleteObtainedUnit(unit: ObtainedUnit, count: number): Promise<void> {
    if (!unit.id || !count) {
      throw new ProgrammingError('ObtainedUnit MUST have an id, and the count MUST be specified');
    }
    const { id } = unit;
    await this._universeGameService.requestWithAutorizationToContext('game', 'post', 'unit/delete', { id, count })
      .pipe(take(1)).toPromise();
  }

  public obtainedUnitToSelectedUnits(obtainedUnits: ObtainedUnit[]): SelectedUnit[] {
    return obtainedUnits.map(current => {
      return {
        id: current.unit.id,
        count: current.count
      };
    });
  }

  /**
   * Find unit upgrade requirements for given logged user faction
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @returns {Observable<UnitUpgradeRequirements[]>}
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

  protected _onUnlockedChange(content: Unit[]): void {
    this._universeGameService.requestWithAutorizationToContext('game', 'get', 'unit/requirements').pipe(take(1)).subscribe(result =>
      this._unitStore.upgradeRequirements.next(result)
    );
    this._unitStore.unlocked.next(content);
  }

  protected _onObtainedChange(content: ObtainedUnit[]): void {
    this._unitStore.obtained.next(
      this._createPlanetsRepresentation(content, (unit) => unit.sourcePlanet.id, true)
    );
  }

  protected _onBuildMissionChange(content: UnitBuildRunningMission[]): void {
    content.forEach(current => DateUtil.computeBrowserTerminationDate(current));
    this._unitStore.runningBuildMissions.next(content);
  }

  private _doComputeRequiredResources(unit: Unit, subscribeToResources: boolean, count = 1): Unit {
    const requirements: ResourceRequirements = new ResourceRequirements();
    requirements.requiredPrimary = unit.primaryResource * count;
    requirements.requiredSecondary = unit.secondaryResource * count;
    requirements.requiredTime = unit.time * count;
    requirements.requiredTime += requirements.handleSustractionPercentage(requirements.requiredTime, this._improvement.moreUnitBuildSpeed);
    requirements.requiredEnergy = (unit.energy || 0) * count;

    this._doCheckResourcesSubscriptionForRequirements(requirements, subscribeToResources);
    unit.requirements = requirements;
    return unit;
  }

  private _doCheckResourcesSubscriptionForRequirements(requirements: ResourceRequirements, subscribeToResources: boolean) {
    if (subscribeToResources) {
      requirements.startDynamicRunnable(this._resourceManagerService);
    } else {
      requirements.checkRunnable(this._resources);
    }
  }

  private _createPlanetsRepresentation<T>(units: T[], keyGetter: (unit: T) => any, isMultiple = true): PlanetsUnitsRepresentation<T[]> {
    const unitsMap: Map<string, T[]> = new Map();
    units.forEach(unit => {
      const planetId: string = keyGetter(unit);
      const collection: T[] = unitsMap.get(planetId);
      if (!collection) {
        unitsMap.set(planetId, [unit]);
      } else {
        collection.push(unit);
      }
    });
    const planetUnitsRepresentation: PlanetsUnitsRepresentation<T[]> = <any>{ planets: {} };
    unitsMap.forEach((value, key) => planetUnitsRepresentation.planets[key] = value);
    return planetUnitsRepresentation;
  }

}
