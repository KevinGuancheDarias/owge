import { MEDIA_ROUTES } from '../../config/config.pojo';
import { Component, OnInit, ViewEncapsulation, Input, ViewChild } from '@angular/core';
import { ModalComponent } from '../modal/modal.component';
import { BaseComponent } from '../../base/base.component';
import { NavigationData } from '../../shared/types/navigation-data.type';
import { PlanetPojo } from '../../shared-pojo/planet.pojo';
import { ObtainedUnit } from '../../shared-pojo/obtained-unit.pojo';
import { UnitService } from '../../service/unit.service';
import { SelectedUnit } from '../../shared/types/selected-unit.type';
import { MissionService } from '../../services/mission.service';
import { MissionType } from '../../shared/types/mission.type';
import { ProgrammingError } from '../../../error/programming.error';
import { PlanetService } from '../../service/planet.service';
import { UnitType } from '../../shared/types/unit-type.type';
import { UnitTypeService } from '../../services/unit-type.service';

@Component({
  selector: 'app-display-quadrant',
  templateUrl: './display-quadrant.component.html',
  styleUrls: ['./display-quadrant.component.less'],
  encapsulation: ViewEncapsulation.None
})
export class DisplayQuadrantComponent extends BaseComponent implements OnInit {

  @Input()
  public navigationData: NavigationData;

  /**
   * Planet to which mission is going to be send
   *
   * @type {PlanetPojo}
   * @memberof DisplayQuadrantComponent
   */
  public selectedPlanet: PlanetPojo;

  public myPlanet: PlanetPojo;

  /**
   * Units in my planet
   *
   * @type {ObtainedUnit[]}
   * @memberof DisplayQuadrantComponent
   */
  public obtainedUnits: ObtainedUnit[];

  public selectedUnits: SelectedUnit[];

  public selectedUnitsTypes: UnitType[];

  public missionType: MissionType = null;

  @ViewChild('missionModal')
  private _missionModal: ModalComponent;

  constructor(
    private _unitService: UnitService,
    private _missionService: MissionService,
    private _planetService: PlanetService,
    private _unitTypeService: UnitTypeService
  ) {
    super();
  }

  public ngOnInit(): void {

    this.loginSessionService.findSelectedPlanet.filter(planet => !!planet).subscribe(planet => {
      this.myPlanet = planet;
    });
  }

  public findImage(planet: PlanetPojo): string {
    return PlanetPojo.findImage(planet);
  }

  public findUiIcon(name: string) {
    return MEDIA_ROUTES.UI_ICONS + name;
  }

  public showMissionDialog(targetPlanet: PlanetPojo): void {
    this.selectedPlanet = targetPlanet;
    this._findObtainedUnits();
    this._missionModal.show();
  }

  public areUnitsSelected(): boolean {
    return this.selectedUnits && this.selectedUnits.some(current => current.count > 0);
  }

  public async sendMission(): Promise<void> {
    await this._runWithLoading(async () => {
      switch (this.missionType) {
        case 'EXPLORE':
          await this._missionService.sendExploreMission(this.myPlanet, this.selectedPlanet, this.selectedUnits).toPromise();
          break;
        case 'GATHER':
          await this._missionService.sendGatherMission(this.myPlanet, this.selectedPlanet, this.selectedUnits).toPromise();
          break;
        case 'ESTABLISH_BASE':
          await this._missionService.sendEstablishBaseMission(this.myPlanet, this.selectedPlanet, this.selectedUnits).toPromise();
          break;
        case 'ATTACK':
          await this._missionService.sendAttackMission(this.myPlanet, this.selectedPlanet, this.selectedUnits).toPromise();
          break;
        case 'COUNTERATTACK':
          await this._missionService.sendCounterattackMission(this.myPlanet, this.selectedPlanet, this.selectedUnits).toPromise();
          break;
        case 'CONQUEST':
          await this._missionService.sendConquestMission(this.myPlanet, this.selectedPlanet, this.selectedUnits).toPromise();
          break;
        case 'DEPLOY':
          await this._missionService.sendDeploy(this.myPlanet, this.selectedPlanet, this.selectedUnits).toPromise();
          break;
        default:
          throw new ProgrammingError(`Unexpected mission type ${this.missionType}`);
      }
      await this._findObtainedUnits();
    });
    this._missionModal.hide();
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
    const retVal = this.selectedUnitsTypes
      ? this._unitTypeService.canDoMission(this.selectedPlanet, this.selectedUnitsTypes, missionType)
      : false;
    return retVal;
  }

  public onSelectedUnitTypes(unitTypes: UnitType[]): void {
    this.selectedUnitsTypes = unitTypes;
    if (!this.selectedUnitsTypes.length || !this.isMissionRealizableByUnitTypes(this.missionType)) {
      this.missionType = null;
    } else if (this.missionType === null && this.isMissionRealizableByUnitTypes('EXPLORE')) {
      this.missionType = 'EXPLORE';
    }
  }
  private async _findObtainedUnits(): Promise<void> {
    this.obtainedUnits = await this._unitService.findInMyPlanet(this.myPlanet.id).toPromise();
  }
}
