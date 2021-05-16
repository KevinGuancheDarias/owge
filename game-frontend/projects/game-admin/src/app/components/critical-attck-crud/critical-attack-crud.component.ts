import { Component, EventEmitter, Input, OnChanges, OnInit, Output, ViewChild } from '@angular/core';
import { CriticalAttack, CriticalAttackEntry, ProgrammingError, UnitType } from '@owge/core';
import { Faction } from '@owge/faction';
import { Unit } from '@owge/universe';
import { WidgetChooseItemModalComponent, WidgetConfirmationDialogComponent, WidgetFilter } from '@owge/widgets';
import { cloneDeep, isEqual } from 'lodash-es';
import { combineLatest } from 'rxjs';
import { take } from 'rxjs/operators';
import { AdminCriticalAttackService } from '../../services/admin-critical-attack.service';
import { AdminFactionService } from '../../services/admin-faction.service';
import { AdminUnitTypeService } from '../../services/admin-unit-type.service';
import { AdminUnitService } from '../../services/admin-unit.service';


interface ValidReference {
  id: number;
  name: string;
}

interface CriticalAttackEntryWithReferenceInfo extends CriticalAttackEntry {
  selectedReference?: ValidReference;
  validReferences?: ValidReference[];
  filteredValidReferences?: ValidReference[];
}


@Component({
  selector: 'app-critical-attack-crud',
  templateUrl: './critical-attack-crud.component.html',
  styleUrls: ['./critical-attack-crud.component.scss']
})
export class CriticalAttackCrudComponent implements OnInit, OnChanges {

  @ViewChild(WidgetChooseItemModalComponent) public modal: WidgetChooseItemModalComponent;
  @ViewChild(WidgetConfirmationDialogComponent) public confirmationDialog: WidgetConfirmationDialogComponent;
  @Input() public beforeDelete: (criticalAttack?: CriticalAttack) => Promise<void>;
  @Input() public criticalAttack: CriticalAttack;
  @Output() public criticalAttackChange: EventEmitter<CriticalAttack> = new EventEmitter;
  @Output() public deleteError: EventEmitter<void> = new EventEmitter;

  public editing: Omit<CriticalAttack, 'entries'> & { entries: CriticalAttackEntryWithReferenceInfo[] };;
  public isChanged = false;
  public areEntriesValid = false;
  public factionFilter: WidgetFilter<Faction>[];
  private _units: Unit[];
  private _unitTypes: UnitType[];

  constructor(
    private _adminFactionService: AdminFactionService,
    private _adminUnitService: AdminUnitService,
    private _adminUnitTypeService: AdminUnitTypeService,
    private _adminCriticalAttackService: AdminCriticalAttackService
  ) { }

  public ngOnInit(): void {
    this._adminFactionService.buildFilter().then(filter => this.factionFilter = [filter]);
  }

  public ngOnChanges(): void {
    this._setEditing();
    this.detectIsChanged();
    combineLatest(
      this._adminUnitService.findAll(),
      this._adminUnitTypeService.findAll(),
      (units, unitTypes) => {
        this._units = units;
        this._unitTypes = unitTypes;
      }
    ).pipe(take(1)).subscribe(() => {
      this.editing.entries.forEach(entry => this.computeTargetForEntry(entry));
    });
  }

  public save(): void {
    const targetMethod: keyof AdminCriticalAttackService = this.editing.id ? 'saveExistingOrPut' : 'saveNew';
    const clearEditingProps: Omit<CriticalAttack, 'entries'> & { entries: CriticalAttackEntryWithReferenceInfo[] } = { ...this.editing };
    clearEditingProps.entries = clearEditingProps.entries.map(current => ({
      referenceId: current.referenceId,
      target: current.target,
      value: current.value
    }));
    this._adminCriticalAttackService[targetMethod](clearEditingProps).subscribe(saved => {
      clearEditingProps.id = saved.id;
      this.criticalAttackChange.emit(saved);
      this.modal.hide();
    });
  }

  public async delete(): Promise<void> {
    try {
      if(this.beforeDelete) {
        await this.beforeDelete(this.editing);
      }
      await this._adminCriticalAttackService.delete(this.editing.id).toPromise();
    }catch (e) {
      this.deleteError.emit();
    }
  }

  public selectedTargetChanged(entry: CriticalAttackEntryWithReferenceInfo): void {
    this.computeTargetForEntry(entry);
    entry.selectedReference = null;
    entry.referenceId = null;
    this.detectIsChanged();
  }

  public selectedReferenceChanged(entry: CriticalAttackEntryWithReferenceInfo): void {
    entry.referenceId = entry.selectedReference?.id;
    this.detectIsChanged();
  }

  public removeEntry(entry: CriticalAttackEntryWithReferenceInfo): void {
    this.editing.entries = this.editing.entries.filter(current => entry !== current);
    this.detectIsChanged();
  }

  public detectIsChanged(): void {
    this.areEntriesValid = this.editing && this.editing.entries.every(entry => entry.target && entry.referenceId);
    this.isChanged = !isEqual(this.editing, this.criticalAttack);
  }

  public computeTargetForEntry(entry: CriticalAttackEntryWithReferenceInfo): void {
    if (entry.target) {
      if (entry.target === 'UNIT') {
        entry.validReferences = this._units;
      } else if (entry.target === 'UNIT_TYPE') {
        entry.validReferences = this._unitTypes;
      } else {
        throw new ProgrammingError(`Value ${entry.target} is not valid`);
      }
      entry.filteredValidReferences = entry.validReferences;
      entry.selectedReference = entry.validReferences.find(ref => ref.id === entry.referenceId);
    }
  }

  public addEntry(): void {
    const entry: CriticalAttackEntryWithReferenceInfo = {
      target: null,
      referenceId: null,
      value: 1,
    };
    this.computeTargetForEntry(entry);
    this.editing.entries.push(entry);
    this.detectIsChanged();
  }

  private _setEditing(): void {
    if (this.criticalAttack) {
      this.editing = cloneDeep(this.criticalAttack);
      if (this._units && this._unitTypes) {
        this.editing.entries.forEach(entry => this.computeTargetForEntry(entry));
      }
    } else {
      this.editing = { id: null, name: '', entries: [] };
    }
  }

}
