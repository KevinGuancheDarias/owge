import { Component, OnInit, OnDestroy } from '@angular/core';

import { AbstractModalContainerComponent, LoggerHelper, ObservableSubscriptionsHelper } from '@owge/core';
import { PlanetService } from '@owge/galaxy';
import { UnitType, MissionStore, Planet, ObtainedUnit, SpeedImpactGroupService } from '@owge/universe';

import { SelectedUnit } from '../shared/types/selected-unit.type';
import { MissionType } from '@owge/core';
import { MissionService } from '../services/mission.service';
import { UnitTypeService } from '../services/unit-type.service';
import { MissionInformationStore } from '../store/mission-information.store';
import { validDeploymentValue } from '../modules/configuration/types/valid-deployment-value.type';
import { ConfigurationService } from '../modules/configuration/services/configuration.service';
import { Observable } from 'rxjs';

/**
 * Modal to send a mission to a planet
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @class MissionModalComponent
 * @extends {AbstractModalContainerComponent}
 */
@Component({
  selector: 'app-mission-modal',
  templateUrl: './mission-modal.component.html',
  styleUrls: ['./mission-modal.component.scss']
})
export class MissionModalComponent extends AbstractModalContainerComponent implements OnInit, OnDestroy {

  /**
   * Planet to which mission is going to be send
   *
   * @type {Planet}
   * @memberof DisplayQuadrantComponent
   */
  public targetPlanet: Planet;

  public sourcePlanet: Planet;

  /**
   * Units that can be used in mission
   *
   * @type {ObtainedUnit[]}
   * @memberof DisplayQuadrantComponent
   */
  public obtainedUnits: ObtainedUnit[];

  public unlockedSpeedImpactGroups: number[] = [];

  public selectedUnits: SelectedUnit[];
  public selectedUnitsTypes: UnitType[];
  public missionType: MissionType = null;
  public isValidSelection = false;
  public maxMissions = 1;
  public deploymentConfig: validDeploymentValue;
  public canCrossGalaxy = true;
  public canDoMissionOutsideGalaxy = true;
  public clickSelectAllBinded: Function;

  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);
  private _subscriptions: ObservableSubscriptionsHelper = new ObservableSubscriptionsHelper;

  constructor(
    private _missionService: MissionService,
    private _planetService: PlanetService,
    private _unitTypeService: UnitTypeService,
    private _missioninformationStore: MissionInformationStore,
    private _configurationService: ConfigurationService,
    private _missionStore: MissionStore,
    private _speedImpactGroupService: SpeedImpactGroupService
  ) {
    super();
  }

  public ngOnInit(): void {
    this._configurationService.observeDeploymentConfiguration().subscribe(val => this.deploymentConfig = val);
    this._missionStore.maxMissions.subscribe(val => this.maxMissions = val);
    this._triggerChange(this._missioninformationStore.originPlanet, sourcePlanet => this.sourcePlanet = sourcePlanet);
    this._triggerChange(this._missioninformationStore.targetPlanet, targetPlanet => this.targetPlanet = targetPlanet);
    this._triggerChange(this._missioninformationStore.availableUnits, availableUnits => this.obtainedUnits = availableUnits);
    this._triggerChange(this._speedImpactGroupService.findunlockedIds(), result => this.unlockedSpeedImpactGroups = result);
  }

  public ngOnDestroy(): void {
    this._subscriptions.unsubscribeAll();
  }

  public areUnitsSelected(): boolean {
    return this.selectedUnits && this.selectedUnits.some(current => current.count > 0);
  }

  /**
   * Sends a mission to the backend
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @returns {Promise<void>}
   * @memberof MissionModalComponent
   */
  public async sendMission(): Promise<void> {
    await this._missionService.sendMission(this.missionType, this.sourcePlanet, this.targetPlanet, this.selectedUnits);
    this._missioninformationStore.missionSent.next(undefined);
    this._childModal.hide();
  }

  public isExplored(planet: Planet): boolean {
    return Planet.isExplored(planet);
  }

  public planetIsMine(planet: Planet): boolean {
    return this._planetService.isMine(planet);
  }

  public hasSelectedMoreThanPossible(): boolean {
    return this.selectedUnits.some(current => {
      const obtainedUnit = this.obtainedUnits.find(currentObtainedUnit => currentObtainedUnit.unit.id === current.unit.id);
      return !current || !obtainedUnit || current.count > obtainedUnit.count;
    });
  }

  public isMissionRealizableByUnitTypes(missionType: MissionType): boolean {
    if (!this.isValidSelection) {
      return true;
    } else if (missionType) {
      const retVal = this.selectedUnitsTypes
        ? this._unitTypeService.canDoMission(this.targetPlanet, this.selectedUnitsTypes, missionType)
        : false;
      return retVal;
    } else {
      return false;
    }
  }

  public onSelectedUnitTypes(unitTypes: UnitType[]): void {
    this.selectedUnitsTypes = unitTypes;
    this.isValidSelection = this.areUnitsSelected();
    if (!this.selectedUnitsTypes.length || !this.isMissionRealizableByUnitTypes(this.missionType)) {
      this.missionType = null;
    } else if (this.missionType === null && this.isMissionRealizableByUnitTypes('EXPLORE')) {
      this.missionType = 'EXPLORE';
    }
  }

  public onUnitSelection(selectedUnits: SelectedUnit[]): void {
    this.selectedUnits = selectedUnits;
    if (this.sourcePlanet.galaxyId !== this.targetPlanet.galaxyId) {
      this.canCrossGalaxy = selectedUnits.map(selectedUnit => selectedUnit.unit.speedImpactGroup)
        .every(speedGroup => !speedGroup || this.unlockedSpeedImpactGroups.some(id => id === speedGroup.id));
      this.onMissionTypeChange();
    } else {
      this.canCrossGalaxy = true;
    }
  }

  public onMissionTypeChange(): void {
    if (this.sourcePlanet.galaxyId !== this.targetPlanet.galaxyId && this.missionType) {
      this.canDoMissionOutsideGalaxy = this._missionService.canDoMission(
        this.targetPlanet,
        this.selectedUnits.map(selectedUnit => selectedUnit.unit.speedImpactGroup),
        this.missionType
      );
    } else {
      this.canDoMissionOutsideGalaxy = true;
    }
    if (this.missionType) {
      this.clickSelectAllBinded = this.selectAllFilter.bind(this);
    }
  }


  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param obtainedUnit
   * @returns
   */
  public async selectAllFilter(obtainedUnit: ObtainedUnit): Promise<boolean> {
    return await this._unitTypeService.canDoMission(
      this.targetPlanet,
      await this._unitTypeService.idsToUnitTypes(obtainedUnit.unit.typeId),
      this.missionType
    );
  }

  /**
   *
   *
   * @param {Planet} targetPlanet
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.4
   * @returns {boolean}
   * @memberof MissionModalComponent
   */
  public isDeploymentAllowed(targetPlanet: Planet): boolean {
    switch (this.deploymentConfig) {
      case 'DISALLOWED':
        return false;
      case 'FREEDOM':
        return true;
      case 'ONLY_ONCE_RETURN_SOURCE':
      case 'ONLY_ONCE_RETURN_DEPLOYED':
        return !this.obtainedUnits.length || !this.obtainedUnits[0].mission || this.planetIsMine(targetPlanet);
      default:
        this._log.warn(`Invalid value for deployment config in the server: ${this.deploymentConfig}, defaulting to FREEDOM`);
        return true;
    }
  }

  private _triggerChange<T>(observable: Observable<T>, subscribeAction: (val: T) => void): void {
    this._subscriptions.add(observable.subscribe(val => {
      subscribeAction(val);
      if (this.selectedUnits) {
        this.onUnitSelection(this.selectedUnits);
      }
    }));
  }
}
