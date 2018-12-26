import { Component, OnInit, Input, ViewChild, Output, EventEmitter } from '@angular/core';

import { AbstractModalContainerComponent } from '../interfaces/abstact-modal-container-component';
import { PlanetPojo } from '../shared-pojo/planet.pojo';
import { ObtainedUnit } from '../shared-pojo/obtained-unit.pojo';
import { SelectedUnit } from '../shared/types/selected-unit.type';
import { UnitType } from '../shared/types/unit-type.type';
import { MissionType } from '../shared/types/mission.type';
import { UnitService } from '../service/unit.service';
import { MissionService } from '../services/mission.service';
import { PlanetService } from '../service/planet.service';
import { UnitTypeService } from '../services/unit-type.service';
import { MissionInformationStore } from '../store/mission-information.store';

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
  styleUrls: ['./mission-modal.component.less']
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

  constructor(
    private _unitService: UnitService,
    private _missionService: MissionService,
    private _planetService: PlanetService,
    private _unitTypeService: UnitTypeService,
    private _missioninformationStore: MissionInformationStore
  ) {
    super();
  }

  public ngOnInit(): void {
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
    if (!this.selectedUnitsTypes.length || !this.isMissionRealizableByUnitTypes(this.missionType)) {
      this.missionType = null;
    } else if (this.missionType === null && this.isMissionRealizableByUnitTypes('EXPLORE')) {
      this.missionType = 'EXPLORE';
    }
  }
}
