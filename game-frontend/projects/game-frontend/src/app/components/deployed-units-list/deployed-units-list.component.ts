import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, ViewChild } from '@angular/core';
import { AsyncCollectionUtil, ModalComponent, ToastrService, UnitType } from '@owge/core';
import { ObtainedUnit, UnitTypeService, SelectedUnit, UnitRuleFinderService, Unit } from '@owge/universe';
import { take } from 'rxjs/operators';
import { UnitSelection } from '../../types/unit-selection.type';

interface UnitsForEachUser {
  username: string;
  obtainedUnits: ObtainedUnit[];
}

@Component({
  selector: 'app-deployed-units-list',
  templateUrl: './deployed-units-list.component.html'
})
export class DeployedUnitsListComponent implements OnInit, OnChanges {
  private static readonly wouldOverpassWarningLiteral = 'APP.DEPLOYED_UNIT_LIST.STORED_UNITS_MODAL.WOULD_OVERPASS_STORED_WEIGHT';

  @Input()
  obtainedUnits: ObtainedUnit[];

  @Input() showUsername = false;
  @Input() showStoredUnits = false;

  /**
   * If can select the unit to be used
   *
   * @type {boolean}
   * @memberof DeployedUnitsListComponent
   */
  @Input() selectable = false;

  @Input() selectAllNotAvailableText = 'APP.DEPLOYED_UNIT_LIST.DEFAULT_NOT_AVAILABLE';
  @Input() filterForAll: (unit: ObtainedUnit) => Promise<boolean>;

  /**
   * Optional, if specified, will display the sustractionf of obtainedUnit.count - <i>finalCount</i>
   *
   * @type {number}
   * @memberof DeployedUnitsListComponent
   */
  @Input() finalCount: number;

  /**
   * Allow to choose if should use tiny cards or not
   *
   * @since 0.8.1
   */
  @Input() useTiny = false;

  @Output() selection: EventEmitter<SelectedUnit[]> = new EventEmitter();

  @Output() unitTypesOfSelection: EventEmitter<UnitType[]> = new EventEmitter();

  @ViewChild('ableToStoreUnitModal') private ableToStoreUnitModal: ModalComponent;
  @ViewChild('storedUnitDisplayModal') private storedUnitDisplayModal: ModalComponent;

  selectionStructure: UnitSelection[] = [];
  availableForStoringStructure: UnitSelection[] = [];
  unitTypes: UnitType[] = [];
  areAllSelected = false;
  unitsForEachUser: UnitsForEachUser[] = [];
  currentSelectionForStoring: UnitSelection;
  unitToDisplayInStoredUnits: ObtainedUnit;

  constructor(
    private _unitTypeService: UnitTypeService,
    private toastrService: ToastrService,
    private unitRuleFinderService: UnitRuleFinderService
  ) { }

  ngOnInit() {
    this.rebuildSelection(this.obtainedUnits, this.selectionStructure);
  }

  ngOnChanges(changes: SimpleChanges) {
    if(changes) {
      if(changes.obtainedUnits?.currentValue) {
        this.areAllSelected = false;
        this.rebuildSelection(this.obtainedUnits, this.selectionStructure);
        this.ableToStoreUnitModal?.hide();
      }

      if (changes.showUsername?.currentValue && this.obtainedUnits) {
        this.unitsForEachUser = [];
        this.obtainedUnits.forEach(unit => {
          const currentObject: UnitsForEachUser = this.unitsForEachUser.find(current => current.username === unit.username);
          if (currentObject) {
            currentObject.obtainedUnits.push(unit);
          } else {
            this.unitsForEachUser.push({
              username: unit.username,
              obtainedUnits: [
                unit
              ]
            });
          }
        });
      }
    }
  }

  async selectionChanged(selection?: UnitSelection, count?: Event): Promise<void> {
    const oldCount = selection?.selectedCount;
    const countInput: HTMLInputElement = count?.target as HTMLInputElement;
    if(selection) {
      selection.selectedCount = parseInt(countInput.value, 10);
    }
    if(this.currentSelectionForStoring) {
      this.recomputeUsedWeight();
    }
    if(selection != null && selection.usedWeight > this.findOutStorageCapacity(selection) ) {
      selection.selectedCount = oldCount;
      countInput.value = oldCount.toString();
      this.toastrService.warn(DeployedUnitsListComponent.wouldOverpassWarningLiteral);
    } else {
      this.areAllSelected = false;
      this.selection.emit(
        this.selectionStructure
          .filter(currentSelection => currentSelection.selectedCount)
          .map(currentSelection => this.mapSelectionToSelected(currentSelection))
      );
      const unitTypeIds: number[] = this.findSelectionWithCount()
        .map(unitSelection => unitSelection.obtainedUnit)
        .map(obtainedUnit => obtainedUnit.unit.typeId)
        .filter(current => current !== null);
      this.unitTypesOfSelection.emit(await this._unitTypeService.idsToUnitTypes(...unitTypeIds));
    }
  }

  async clickSelectAll(): Promise<void> {
    if (this.filterForAll) {
      let selectionChanged = false;
      await Promise.all(this.obtainedUnits.map(async obtainedUnit => {
        if (await this.filterForAll(obtainedUnit)) {
          selectionChanged = true;
          this.selectionStructure
            .find(current => current.obtainedUnit.id === obtainedUnit.id)
            .selectedCount = obtainedUnit.count;
        }
      }));
      if (selectionChanged) {
        await this.selectionChanged();
      }
      this.areAllSelected = true;
    } else {
      this.toastrService.error(this.selectAllNotAvailableText);
    }
  }

  clickSelectAllOfUnit(selection: UnitSelection): Promise<void> {
    selection.selectedCount = selection.obtainedUnit.count;
    return this.selectionChanged();
  }

  clickUnselectAll(): void {
    this.selectionStructure.forEach(currentSelection => {
      delete currentSelection.selectedCount;
    });
    this.areAllSelected = false;
  }

  async doShowStoredUnits(currentSelection: UnitSelection): Promise<void> {
    if(currentSelection.selectedCount) {
      this.currentSelectionForStoring = currentSelection;
      const filteredObtainedUnits: ObtainedUnit[] = await this.filterOutNotStorableUnits(this.obtainedUnits);
      this.rebuildSelection(filteredObtainedUnits, this.currentSelectionForStoring.storedUnitsSelection);
      this.ableToStoreUnitModal.show();
    } else {
      this.toastrService.warn('APP.DEPLOYED_UNIT_LIST.STORED_UNITS_MODAL.NO_COUNT_SELECTED');
    }
  }

  updateStoredCount(currentSelection: UnitSelection, event: Event): void {
    const oldCount: number = currentSelection.selectedCount;
    const countInput: HTMLInputElement = event.target as HTMLInputElement;
    currentSelection.selectedCount = parseInt(countInput.value, 10);
    this.recomputeUsedWeight();
    if(this.currentSelectionForStoring.usedWeight > this.findOutStorageCapacity(this.currentSelectionForStoring)) {
      currentSelection.selectedCount = oldCount;
      countInput.value = oldCount.toString();
      this.recomputeUsedWeight();
      this.toastrService.warn(DeployedUnitsListComponent.wouldOverpassWarningLiteral);
    } else {
      this.selectionChanged();
    }
  }

  doDisplayStoredUnits(unitToDisplay: ObtainedUnit): void {
    this.unitToDisplayInStoredUnits = unitToDisplay;
    this.storedUnitDisplayModal.show();
  }

  clickSaveStoredUnits(): void {
    this.ableToStoreUnitModal.hide();
  }

  private filterOutNotStorableUnits(obtainedUnits: ObtainedUnit[]): Promise<ObtainedUnit[]> {
    const from: Unit = this.currentSelectionForStoring.obtainedUnit.unit;
    return AsyncCollectionUtil.filter(obtainedUnits, async obtainedUnit =>
      (await this.unitRuleFinderService.findUnitRuleFromTo(
        'UNIT_STORES_UNIT', from, obtainedUnit.unit
        ).pipe(take(1)).toPromise()
      ) !== null
    );
  }

  private mapSelectionToSelected(currentSelection: UnitSelection): SelectedUnit {
    return {
      id: currentSelection.obtainedUnit.unit.id,
      count: currentSelection.selectedCount,
      unit: currentSelection.obtainedUnit.unit,
      expirationId: currentSelection.obtainedUnit?.temporalInformation?.id,
      storedUnits: currentSelection.storedUnitsSelection
        .filter(storedUnit => storedUnit.selectedCount)
        .map(storedUnit => this.mapSelectionToSelected(storedUnit))
    };
  }

  private findOutStorageCapacity(currentSelection: UnitSelection): number {
    return currentSelection.selectedCount * currentSelection.obtainedUnit.unit.storageCapacity;
  }

  private recomputeUsedWeight(): void {
    this.currentSelectionForStoring.usedWeight = this.currentSelectionForStoring.storedUnitsSelection
      .filter(currentStored => currentStored.selectedCount)
      .map(currentStored => currentStored.selectedCount * currentStored.obtainedUnit.unit.storedWeight)
      .reduce((sum,current) => sum + current, 0);
  }

  private rebuildSelection(obtainedUnits: ObtainedUnit[], selectionStructure: UnitSelection[]) {
    obtainedUnits.forEach(current => {
      const selectionCurrentValue = selectionStructure.find(selection => selection.obtainedUnit.id === current.id);
      if(selectionCurrentValue) {
        selectionCurrentValue.obtainedUnit = current;
        if(selectionCurrentValue.selectedCount && selectionCurrentValue.selectedCount > current.count) {
          selectionCurrentValue.selectedCount = current.count;
        }
      } else {
        selectionStructure.push({
          obtainedUnit: current,
          storedUnitsSelection: [],
          usedWeight: 0
        });
      }
    });
  }

  private findSelectionWithCount(): UnitSelection[] {
    return this.selectionStructure.filter(current => current.selectedCount);
  }
}
