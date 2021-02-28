import { Component, OnInit, Input, ViewChild, Output, EventEmitter, OnChanges } from '@angular/core';
import { ModalComponent, ProgrammingError, LoggerHelper, UnitType, AttackRuleEntry, AttackRule } from '@owge/core';
import { WidgetConfirmationDialogComponent, WidgetFilter } from '@owge/widgets';
import { AdminAttackRuleService } from '../../services/admin-attack-rule.service';
import { isEqual } from 'lodash-es';
import { AdminUnitService } from '../../services/admin-unit.service';
import { AdminUnitTypeService } from '../../services/admin-unit-type.service';
import { Unit } from '@owge/universe';
import { combineLatest } from 'rxjs';
import { take } from 'rxjs/operators';
import { AdminFactionService } from '../../services/admin-faction.service';
import { Faction } from '@owge/faction';
import { cloneDeep } from 'lodash-es';

interface ValidReference {
  id: number;
  name: string;
}

interface AttackRuleEntryWithReferenceInfo extends AttackRuleEntry {
  selectedReference?: ValidReference;
  validReferences?: ValidReference[];
  filteredValidReferences?: ValidReference[];
}

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-attack-rule-crud',
  templateUrl: './attack-rule-crud.component.html',
  styleUrls: ['./attack-rule-crud.component.scss']
})
export class AttackRuleCrudComponent implements OnInit, OnChanges {
  @ViewChild(ModalComponent) public modal: ModalComponent;
  @ViewChild(WidgetConfirmationDialogComponent) public confirmationDialog: WidgetConfirmationDialogComponent;
  @Input() public attackRule: AttackRule;
  @Input() public beforeDelete: (attackRule?: AttackRule) => Promise<void>;
  @Output() public attackRuleChange: EventEmitter<AttackRule> = new EventEmitter;
  @Output() public deleteError: EventEmitter<void> = new EventEmitter;

  public editing: Omit<AttackRule, 'entries'> & { entries: AttackRuleEntryWithReferenceInfo[] };
  public isChanged = false;
  public factionFilter: WidgetFilter<Faction>[];
  public areEntriesValid = false;

  private _units: Unit[];
  private _unitTypes: UnitType[];
  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);

  constructor(
    private _adminFactionService: AdminFactionService,
    private _adminAttackRuleService: AdminAttackRuleService,
    private _adminUnitService: AdminUnitService,
    private _adminUnitTypeService: AdminUnitTypeService
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

  public showModal() {
    this._setEditing();
    this.modal.show();
  }

  public addEntry(): void {
    const entry: AttackRuleEntryWithReferenceInfo = {
      target: null,
      referenceId: null,
      canAttack: false,
    };
    this.computeTargetForEntry(entry);
    this.editing.entries.push(entry);
    this.detectIsChanged();
  }
  public computeTargetForEntry(entry: AttackRuleEntryWithReferenceInfo): void {
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

  public selectedTargetChanged(entry: AttackRuleEntryWithReferenceInfo): void {
    this._log.debug('Target changed to : ' + entry.target);
    this.computeTargetForEntry(entry);
    entry.selectedReference = null;
    entry.referenceId = null;
    this.detectIsChanged();
  }

  public selectedReferenceChanged(entry: AttackRuleEntryWithReferenceInfo): void {
    entry.referenceId = entry.selectedReference?.id;
    this.detectIsChanged();
  }

  public removeEntry(entry: AttackRuleEntryWithReferenceInfo): void {
    this.editing.entries = this.editing.entries.filter(current => entry !== current);
    this.detectIsChanged();
  }

  public save(): void {
    const targetMethod: keyof AdminAttackRuleService = this.editing.id ? 'saveExistingOrPut' : 'saveNew';
    const clearEditingProps: Omit<AttackRule, 'entries'> & { entries: AttackRuleEntryWithReferenceInfo[] } = { ...this.editing };
    clearEditingProps.entries = clearEditingProps.entries.map(current => ({
      referenceId: current.referenceId,
      target: current.target,
      canAttack: current.canAttack
    }));
    this._adminAttackRuleService[targetMethod](clearEditingProps).subscribe(saved => {
      clearEditingProps.id = saved.id;
      this.attackRuleChange.emit(saved);
      this.modal.hide();
    });
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   */
  public async delete(): Promise<void> {
    try {
      if (this.beforeDelete) {
        await this.beforeDelete(this.attackRule);
      }
      await this._adminAttackRuleService.delete(this.attackRule.id).pipe(take(1)).toPromise();
      this.attackRuleChange.emit(null);
    } catch (_) {
      this.deleteError.emit();
    }

  }

  public detectIsChanged(): void {
    this.areEntriesValid = this.editing && this.editing.entries.every(entry => entry.target && entry.referenceId);
    this.isChanged = !isEqual(this.editing, this.attackRule);
  }

  private _setEditing(): void {
    if (this.attackRule) {
      this.editing = cloneDeep(this.attackRule);
      if (this._units && this._unitTypes) {
        this.editing.entries.forEach(entry => this.computeTargetForEntry(entry));
      }
    } else {
      this.editing = { id: null, name: '', entries: [] };
    }
  }
}
