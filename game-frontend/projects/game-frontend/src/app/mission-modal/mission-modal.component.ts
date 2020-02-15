import { Component, OnInit } from '@angular/core';

import { AbstractModalContainerComponent, LoggerHelper } from '@owge/core';
import { PlanetService } from '@owge/galaxy';
import { UnitType, MissionStore } from '@owge/universe';

import { PlanetPojo } from '../shared-pojo/planet.pojo';
import { ObtainedUnit } from '../shared-pojo/obtained-unit.pojo';
import { SelectedUnit } from '../shared/types/selected-unit.type';
import { MissionType } from '../shared/types/mission.type';
import { MissionService } from '../services/mission.service';
import { UnitTypeService } from '../services/unit-type.service';
import { MissionInformationStore } from '../store/mission-information.store';
import { validDeploymentValue } from '../modules/configuration/types/valid-deployment-value.type';
import { ConfigurationService } from '../modules/configuration/services/configuration.service';

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
export class MissionModalComponent extends AbstractModalContainerComponent implements OnInit {

  /**
   * Planet to which mission is going to be send
   *
   * @type {PlanetPojo}
   * @memberof DisplayQuadrantComponent
   */
  public targetPlanet: PlanetPojo;

  public sourcePlanet: PlanetPojo;

  /**
   * Units that can be used in mission
   *
   * @type {ObtainedUnit[]}
   * @memberof DisplayQuadrantComponent
   */
  public obtainedUnits: ObtainedUnit[];

  public selectedUnits: SelectedUnit[];
  public selectedUnitsTypes: UnitType[];
  public missionType: MissionType = null;
  public isValidSelection = false;
  public maxMissions = 1;
  public deploymentConfig: validDeploymentValue;

  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);

  constructor(
    private _missionService: MissionService,
    private _planetService: PlanetService,
    private _unitTypeService: UnitTypeService,
    private _missioninformationStore: MissionInformationStore,
    private _configurationService: ConfigurationService,
    private _missionStore: MissionStore
  ) {
    super();
  }

  public ngOnInit(): void {
    this._configurationService.observeDeploymentConfiguration().subscribe(val => this.deploymentConfig = val);
    this._missionStore.maxMissions.subscribe(val => this.maxMissions = val);
    this._missioninformationStore.originPlanet.subscribe(sourcePlanet => this.sourcePlanet = sourcePlanet);
    this._missioninformationStore.targetPlanet.subscribe(targetPlanet => this.targetPlanet = targetPlanet);
    this._missioninformationStore.availableUnits.subscribe(availableUnits => this.obtainedUnits = availableUnits);
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

  public isExplored(planet: PlanetPojo): boolean {
    return PlanetPojo.isExplored(planet);
  }

  public planetIsMine(planet: PlanetPojo): boolean {
    return this._planetService.isMine(planet);
  }

  public hasSelectedMoreThanPossible(): boolean {
    return this.selectedUnits.some(current => {
      const obtainedUnit = this.obtainedUnits.find(currentObtainedUnit => currentObtainedUnit.unit.id === current.id);
      return !current || !obtainedUnit || current.count > obtainedUnit.count;
    });
  }

  public isMissionRealizableByUnitTypes(missionType: MissionType): boolean {
    if (missionType) {
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

  /**
   *
   *
   * @param {PlanetPojo} targetPlanet
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.7.4
   * @returns {boolean}
   * @memberof MissionModalComponent
   */
  public isDeploymentAllowed(targetPlanet: PlanetPojo): boolean {
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
}
