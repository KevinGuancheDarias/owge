import { Component, Input, OnChanges, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { take } from 'rxjs/operators';

import { WithRequirementsCrudMixin, RequirementInformation } from '@owge/universe';
import { ProgrammingError, ModalComponent, CommonEntity } from '@owge/core';
import { DisplayService } from '@owge/widgets';

import { AdminFactionService } from '../../services/admin-faction.service';
import { AdminUpgradeService } from '../../services/admin-upgrade.service';


interface RequirementInformationWithTranslation extends RequirementInformation {
  translatedDescription: string;
  resolvedName: string;
  targetSecondValueTranslation?: string;
  targetThirdValueTranslation?: string;
}

@Component({
  selector: 'app-object-requirements-crud',
  templateUrl: './object-requirements-crud.component.html',
  styleUrls: ['./object-requirements-crud.component.less']
})
export class ObjectRequirementsCrudComponent implements OnChanges {

  @Input() public entityId: any;
  @Input() public service: WithRequirementsCrudMixin<any>;

  public requirements: RequirementInformationWithTranslation[];
  public newRequirement: RequirementInformationWithTranslation;

  public secondValueList: CommonEntity<number>[];

  @ViewChild('requirementsModal', { static: true }) protected _requirementsModal: ModalComponent;

  public constructor(
    private _translateService: TranslateService,
    private _adminFactionService: AdminFactionService,
    private _displayService: DisplayService,
    private _adminUpgradeService: AdminUpgradeService
  ) { }

  public ngOnChanges() {
    if (this.entityId) {
      this._loadRequirements();
    }
  }

  public add() {
    this.newRequirement = <any>{ requirement: {} };
    this._requirementsModal.show();
  }

  public cancel() {
    this._requirementsModal.hide();
  }

  public findRequirementDescription(code: string): Observable<string> {
    return this._translateService.get(`REQUIREMENTS.DESCRIPTIONS.${code}`);
  }

  public loadSecondValue() {
    delete this.newRequirement.secondValue;
    if (this.newRequirement.requirement.code) {
      this.secondValueList = null;
      delete this.newRequirement.targetThirdValueTranslation;
      this._translateService.get(`REQUIREMENTS.TARGETS.${this.newRequirement.requirement.code}`)
        .subscribe(val => this.newRequirement.targetSecondValueTranslation = val);
      switch (this.newRequirement.requirement.code) {
        case 'BEEN_RACE':
          this._adminFactionService.findAll().subscribe(factions => this.secondValueList = factions);
          break;
        case 'UPGRADE_LEVEL':
          this._adminUpgradeService.findAll().subscribe(upgrades => this.secondValueList = upgrades);
          this._translateService.get('REQUIREMENTS.THIRD_VALUES.UPGRADE_LEVEL')
            .subscribe(translation => this.newRequirement.targetThirdValueTranslation = translation);
          break;
        default:
          throw new ProgrammingError(`The requirement with code ${this.newRequirement.requirement.code} is not supported`);
      }
    } else {
      this.newRequirement.targetSecondValueTranslation = '';
    }
  }

  public canAdd(): boolean {
    return this.newRequirement.targetSecondValueTranslation &&
      !!((this.newRequirement.targetThirdValueTranslation && this.newRequirement.secondValue && this.newRequirement.thirdValue)
        || (!this.newRequirement.targetThirdValueTranslation && this.newRequirement.secondValue));
  }

  public save(): void {
    const toSave: RequirementInformationWithTranslation = { ...this.newRequirement };
    delete toSave.targetSecondValueTranslation;
    delete toSave.targetThirdValueTranslation;
    this.service.saveRequirement(this.entityId, toSave).subscribe(saved => {
      this.requirements.push(this._handleRequirementTranslation(saved));
      this._requirementsModal.hide();
    });

  }

  public delete(requirementInformation: RequirementInformation): void {
    this._translateService.get('CRUD.CONFIRM_DELETE', { elName: '' }).subscribe(async val => {
      if (await this._displayService.confirm(val)) {
        this.service.deleteRequirement(this.entityId, requirementInformation.id).subscribe(() => {
          this.requirements = this.requirements.filter(current => current.id !== requirementInformation.id);
        });
      }
    });
  }

  private _loadRequirements(): void {
    this.service.findRequirements(this.entityId).subscribe(requirements =>
      this.requirements = requirements.map(current => this._handleRequirementTranslation(current))
    );
  }

  private _handleRequirementTranslation(requirementInformation: RequirementInformation): RequirementInformationWithTranslation {
    const retVal = {
      ...requirementInformation,
      translatedDescription: 'Loading...',
      resolvedName: 'Loading...'
    };
    this._findTranslatedDescription(requirementInformation).subscribe(result => retVal.translatedDescription = result);
    this._resolveTargetName(requirementInformation).then(result => retVal.resolvedName = result);
    return retVal;
  }

  private _findTranslatedDescription(requirementInformation: RequirementInformation): Observable<string> {
    return this.findRequirementDescription(requirementInformation.requirement.code);
  }

  private async _resolveTargetName(requirementInformation: RequirementInformation): Promise<string> {
    switch (requirementInformation.requirement.code) {
      case 'BEEN_RACE':
        return (await this._adminFactionService.findOneById(requirementInformation.secondValue).pipe(take(1)).toPromise()).name;
      case 'UPGRADE_LEVEL':
        return (await this._adminUpgradeService.findOneById(requirementInformation.secondValue).pipe(take(1)).toPromise()).name;
      default:
        throw new ProgrammingError(`Invalid requirement code ${requirementInformation.requirement.code}`);
    }
  }
}
