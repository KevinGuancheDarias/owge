import { Component, HostBinding, Input, OnChanges, ViewChild } from '@angular/core';
import { Improvement, ImprovementUnitType, LoadingService, ModalComponent, UnitType } from '@owge/core';
import { WithImprovementsCrudMixin } from '@owge/universe';
import { isEqual } from 'lodash-es';
import { take } from 'rxjs/operators';
import { AdminUnitTypeService } from '../../services/admin-unit-type.service';


@Component({
  selector: 'app-object-improvements-crud',
  templateUrl: './object-improvements-crud.component.html',
  styleUrls: ['./object-improvements-crud.component.less']
})
export class ObjectImprovementsCrudComponent implements OnChanges {

  @Input() public entityId: any;
  @Input() public service: WithImprovementsCrudMixin<any>;

  public entityImprovement: Improvement;
  public modificableImprovement: Improvement;
  public improvementUnitTypes: ImprovementUnitType[] = null;
  public isChanged: boolean;
  public unitTypes: UnitType[];
  public newImprovementUnitType: ImprovementUnitType;
  public selectedUnitType: UnitType;

  @ViewChild('improvementsUnitTypesModal', { static: true }) protected _improvementsUnitTypesModal: ModalComponent;
  @ViewChild('improvementsModal', { static: true }) protected _improvementsModal: ModalComponent;
  @HostBinding('class') private readonly _classes = 'row owge-theme-base-colors';

  constructor(private _loadingService: LoadingService, private _adminUnitTypeService: AdminUnitTypeService) {

  }

  public ngOnChanges() {
    if (this.entityId) {
      this._loadImprovements();
    } else {
      this.modificableImprovement = null;
      this.entityImprovement = null;
    }
  }

  /**
   *
   * @todo In the future don't invoke this method from the template
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   */
  public detectIsChanged(): boolean {
    this.isChanged = !isEqual(this.entityImprovement, this.modificableImprovement);
    return this.isChanged;
  }

  public clickSave(): void {
    this._loadingService.runWithLoading(async () => {
      const saved = await this.service.saveImprovement(this.entityId, this.modificableImprovement).pipe(take(1)).toPromise();
      this.entityImprovement = saved;
      this.modificableImprovement = { ...saved };
    });
  }

  public clickModifyUnitTypeImprovements(): void {
    this._loadingService.runWithLoading(async () => {
      this.improvementUnitTypes = await this.service.findImprovementUnitTypes(this.entityId).pipe(take(1)).toPromise();
      this._improvementsModal.show();
    });
  }

  public clickDelete(improvementUnitType: ImprovementUnitType): void {
    this._loadingService.runWithLoading(async () => {
      await this.service.deleteImprovementUnitType(this.entityId, improvementUnitType.id).pipe(take(1)).toPromise();
      this.improvementUnitTypes = this.improvementUnitTypes.filter(current => current.id !== improvementUnitType.id);
    });
  }

  public clickCloseImprovements(): void {
    this._improvementsModal.hide();
  }

  public clickAddUnitTypeImprovement(): void {
    this._loadingService.runWithLoading(async () => {
      this.unitTypes = await this._adminUnitTypeService.findAll().pipe(take(1)).toPromise();
      this.newImprovementUnitType = <any>{};
      this._improvementsUnitTypesModal.show();
    });
  }

  public clickSaveUnitTypeImprovement(): void {
    this.newImprovementUnitType.unitTypeName = this.unitTypes.find(current => current.id === this.newImprovementUnitType.unitTypeId).name;
    this._loadingService.runWithLoading(async () => {
      const saved: ImprovementUnitType = await this.service.saveImprovementUnitType(this.entityId, this.newImprovementUnitType)
        .pipe(take(1)).toPromise();
      if (saved) {
        this.improvementUnitTypes.push(saved);
      }
      this._improvementsUnitTypesModal.hide();
    });
  }

  public clickCancelUnitTypeImprovement(): void {
    this._improvementsUnitTypesModal.hide();
  }

  public canAdd(): boolean {
    return !!(this.newImprovementUnitType.type && this.newImprovementUnitType.unitTypeId && this.newImprovementUnitType.value);
  }

  public onChangeSelectedUnitType(): void {
    this.selectedUnitType = this.unitTypes.find(current => current.id === this.newImprovementUnitType.unitTypeId);
  }

  private _loadImprovements(): void {
    this.service.findImprovement(this.entityId).subscribe(improvement => {
      this.entityImprovement = improvement;
      this.modificableImprovement = { ...improvement };
    });
  }
}
