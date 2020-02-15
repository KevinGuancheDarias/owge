import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { filter, take } from 'rxjs/operators';
import { Observable, BehaviorSubject, Subscription } from 'rxjs';

import { ProgrammingError, UserStorage, User, Improvement } from '@owge/core';
import { ClockSyncService, UniverseGameService } from '@owge/universe';
import { PlanetService, PlanetStore } from '@owge/galaxy';

import { ObtainedUnit } from '../shared-pojo/obtained-unit.pojo';
import { ResourcesEnum } from '../shared-enum/resources-enum';
import { RequirementPojo } from './../shared-pojo/requirement.pojo';
import { RunningUnitPojo } from './../shared-pojo/running-unit-build.pojo';
import { PlanetPojo } from './../shared-pojo/planet.pojo';
import { UnitPojo } from './../shared-pojo/unit.pojo';
import { ResourceManagerService } from './resource-manager.service';
import { UnitUpgradeRequirements } from '../shared/types/unit-upgrade-requirements.type';
import { UnitTypeService } from '../services/unit-type.service';
import { SelectedUnit } from '../shared/types/selected-unit.type';
import { AutoUpdatedResources } from '../class/auto-updated-resources';

export class PlanetsNotReadyError extends Error { }

export class RunningUnitIntervalInformation {
  public interval: number;
  public missionData: WithCalculatedDateRunningUnitBuild;

  constructor(interval: number, missionData: WithCalculatedDateRunningUnitBuild) {
    this.interval = interval;
    this.missionData = missionData;
  }
}

export class WithCalculatedDateRunningUnitBuild extends RunningUnitPojo {
  public terminationDate: Date;
}

@Injectable()
export class UnitService {

  private _planetList: PlanetPojo[];

  /** @var {number} provides access to each interval, of running build mission (if any), the key value is like, planetId => intervalId */
  private _intervals: RunningUnitIntervalInformation[] = [];

  /**
   * Some functions MUST only be invoked when the planets has been loaded <br>
   * For example: Asking if there is a running unit recluit mission, can't be done <br />
   * This behavior subject value is true when the list has been loaded
   *
   * @author Kevin Guanche Darias
   */
  public get planetsLoaded(): BehaviorSubject<boolean> {
    return this._planetsLoaded;
  }
  private _planetsLoaded: BehaviorSubject<boolean> = new BehaviorSubject(false);

  public get ready(): Observable<boolean> {
    return this._ready.asObservable();
  }
  private _ready: BehaviorSubject<boolean> = new BehaviorSubject(false);

  private _selectedPlanet: PlanetPojo;
  private _resources: AutoUpdatedResources;
  private _improvement: Improvement;

  constructor(
    private _resourceManagerService: ResourceManagerService,
    private _planetService: PlanetService,
    private _unitTypeService: UnitTypeService,
    private _clockSyncService: ClockSyncService,
    private _universeGameService: UniverseGameService,
    private _planetStore: PlanetStore,
    private _userStore: UserStorage<User>
  ) {
    this._userStore.currentUserImprovements.pipe(take(1)).subscribe(improvement => this._improvement = improvement);
    this._resources = new AutoUpdatedResources(_resourceManagerService);
    this._subscribeToPlanetChanges();
    this._planetStore.selectedPlanet.subscribe(currentSelected => this._selectedPlanet = currentSelected);
  }

  /**
   * Returns the list of unlocked units!
   *
   * @author Kevin Guanche Darias
   */
  public findUnlocked(): Observable<UnitPojo[]> {
    return this._universeGameService.getWithAuthorizationToUniverse('unit/findUnlocked');
  }

  /**
   * Checks if there is a unit building in the selected planet<br>
   * <b>IMPORTANT:</b> Can only be used after planets has been loaded
   *
   * @author Kevin Guanche Darias
   */
  public findIsRunningInSelectedPlanet(): RunningUnitIntervalInformation {
    return this._findRunningBuildWithData(this._selectedPlanet.id);
  }

  /**
   * Computes required resources by the next upgrade level
   *
   * @param {UnitPojo} unit
   *          - Notice: this function alters this object
   * @param {boolean} subscribeToResources true if want to recompute the runnable field of RequirementPojo,
   *          on each change to the resources (expensive!)
   * @param {BehaviorSubject<number>} countBehabiorSubject - Specify it to automatically update resource requirements on changes to count
   * @returns obtainedUpgrade with filled values
   * @author Kevin Guanche Darias
   */
  public computeRequiredResources(unit: UnitPojo, subscribeToResources: boolean, countBehaviorSubject: BehaviorSubject<number>): UnitPojo {
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
          unit.requirements = new RequirementPojo();
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
   * @param {UnitPojo} unit to be build
   * @param {number} count  of that unit
   * @todo Finish it!
   * @author Kevin Guanche Darias
   */
  public registerUnitBuild(unit: UnitPojo, count: number): void {
    let params: HttpParams = new HttpParams();
    unit = this._doComputeRequiredResources(unit, false, count);
    params = params.append('planetId', this._selectedPlanet.id.toString());
    params = params.append('unitId', unit.id.toString());
    params = params.append('count', count.toString());
    this._universeGameService.postWithAuthorizationToUniverse('unit/build', '', { params }).subscribe(res => {
      this._resourceManagerService.minusResources(ResourcesEnum.PRIMARY, unit.requirements.requiredPrimary);
      this._resourceManagerService.minusResources(ResourcesEnum.SECONDARY, unit.requirements.requiredSecondary);
      this._resourceManagerService.addResources(ResourcesEnum.CONSUMED_ENERGY, unit.requirements.requiredEnergy);
      this._unitTypeService.addToType(unit.typeId, count);
      if (res) {
        res.terminationDate = this._clockSyncService.computeSyncedTerminationDate(res.terminationDate);
        this._refreshPlanetsLoaded();
      }
    });
  }

  /**
   * Cancels a unit build mission
   *
   * @todo https://trello.com/c/WPx0qaXR/23-unitservicecancel-should-not-call-thissubscribetoplanetchanges
   * @param {RunningUnitPojo} missionData
   * @memberof UnitService
   * @author Kevin Guanche Darias
   */
  public cancel(missionData: RunningUnitPojo) {
    let params: HttpParams = new HttpParams();
    params = params.append('missionId', missionData.missionId);
    this._universeGameService.getWithAuthorizationToUniverse('unit/cancel', { params }).subscribe(() => {
      this._resourceManagerService.addResources(ResourcesEnum.PRIMARY, missionData.requiredPrimary);
      this._resourceManagerService.addResources(ResourcesEnum.SECONDARY, missionData.requiredSecondary);
      this._resourceManagerService.minusResources(ResourcesEnum.CONSUMED_ENERGY, missionData.unit.energy * missionData.count);
      this._subscribeToPlanetChanges();
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
    let params: HttpParams = new HttpParams();
    params = params.append('planetId', planetId.toString());
    return this._universeGameService.getWithAuthorizationToUniverse('unit/findInMyPlanet', { params });
  }


  /**
   * Find unit upgrade requirements for given logged user faction
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @returns {Observable<UnitUpgradeRequirements[]>}
   * @memberof UnitService
   */
  public findUnitUpgradeRequirements(): Observable<UnitUpgradeRequirements[]> {
    return this._universeGameService.getWithAuthorizationToUniverse('unit/requirements');
  }


  /**
   * Deletes specified obtainedUnit, if count is exactly the totally available, will completely remove the obtainedUnit
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {ObtainedUnit} unit Should contain at least the id, and the count to delete
   * @returns {Promise<void>}
   * @throws {ProgrammingError} Invalid unit was passed
   * @memberof UnitService
   */
  public async deleteObtainedUnit(unit: ObtainedUnit): Promise<void> {
    if (!unit.id || !unit.count) {
      throw new ProgrammingError('ObtainedUnit MUST have an id, and the count MUST be specified');
    }
    const { id, count } = unit;
    await this._universeGameService.postWithAuthorizationToUniverse('unit/delete', { id, count }).toPromise();
    this._unitTypeService.sustractToType(unit.unit.typeId, count);
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
   * Will listen to planet changes, that the planet service has emmited! <br />
   * And clears and register new intervals (Asking the server if there is a new mission)
   *
   * @author Kevin Guanche Darias
   */
  private _subscribeToPlanetChanges(): void {
    this.planetsLoaded.next(false);
    this._planetService.myPlanets.pipe(filter(myPlanets => !!myPlanets)).subscribe(async myPlanets => {
      this._clearIntervals();
      this._planetList = myPlanets;
      await this._registerIntervals(myPlanets);
      this._planetsLoaded.next(true);
    });
  }

  private _clearIntervals(): void {
    this._intervals.forEach(value => window.clearInterval(value.interval));
    this._intervals = [];
  }

  /**
   * This method will resolve when all planets has been queried
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @private
   * @todo In the future refactor this method, NOT TO BE SO COMPREX
   * @param {PlanetPojo[]} planets
   * @returns {Promise<any>}
   * @memberof UnitService
   */
  private _registerIntervals(planets: PlanetPojo[]): Promise<any> {
    return Promise.all(planets.map<Promise<void>>(currentPlanet => {
      return new Promise(resolve => {
        this._findRunningBuild(currentPlanet).subscribe(runningMission => {
          if (runningMission) {
            runningMission.terminationDate = new Date(runningMission.terminationDate);
          }
          resolve();
        });
      });
    }));
  }

  /**
   * Will ask the server if the selected planet has a unit build mission going!
   *
   * @param {PlanetPojo} planet - Planet to ask for
   * @author Kevin Guanche Darias
   */
  private _findRunningBuild(planet: PlanetPojo): Observable<WithCalculatedDateRunningUnitBuild> {
    let params: HttpParams = new HttpParams();
    params = params.append('planetId', planet.id.toString());
    return this._universeGameService.getWithAuthorizationToUniverse('unit/findRunning', { params });
  }

  /**
   * From the registered intervals, returns the data
   */
  private _findRunningBuildWithData(planetId: number): RunningUnitIntervalInformation {
    if (!this.planetsLoaded.value) {
      throw new PlanetsNotReadyError('Can\'t invoke this method when planets has not been loaded!');
    }

    for (const currentPlanetId in this._intervals) {
      if (currentPlanetId === planetId.toString()) {
        return this._intervals[currentPlanetId];
      }
    }
    return null;
  }

  private _doComputeRequiredResources(unit: UnitPojo, subscribeToResources: boolean, count = 1): UnitPojo {
    const requirements: RequirementPojo = new RequirementPojo();
    requirements.requiredPrimary = unit.primaryResource * count;
    requirements.requiredSecondary = unit.secondaryResource * count;
    requirements.requiredTime = unit.time * count;
    requirements.requiredTime += requirements.handleSustractionPercentage(requirements.requiredTime, this._improvement.moreUnitBuildSpeed);
    requirements.requiredEnergy = (unit.energy || 0) * count;

    this._doCheckResourcesSubscriptionForRequirements(requirements, subscribeToResources);
    unit.requirements = requirements;
    return unit;
  }

  private _doCheckResourcesSubscriptionForRequirements(requirements: RequirementPojo, subscribeToResources: boolean) {
    if (subscribeToResources) {
      requirements.startDynamicRunnable(this._resourceManagerService);
    } else {
      requirements.checkRunnable(this._resources);
    }
  }

  /**
   * When a planet change his conditions, for example because it's now recluiting, this will force BehaviorSubject to fire again
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @private
   * @memberof UnitService
   */
  private _refreshPlanetsLoaded(): void {
    this._planetsLoaded.next(false);
    this._planetsLoaded.next(true);
  }
}
