import { Component, Input, OnChanges, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

import { WithRequirementsCrudMixin, RequirementInformation } from '@owge/universe';
import { ModalComponent, CommonEntity } from '@owge/core';
import { DisplayService } from '@owge/widgets';

import { RequirementInformationWithTranslation } from '../../types/requirement-information-with-translation.type';
import { RequirementsModalComponent } from '../requirements-modal/requirements-modal.component';
import { AdminRequirementService } from '../../services/admin-requirement.service';

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

  @ViewChild(RequirementsModalComponent) protected _requirementsModal: ModalComponent;

  public constructor(
    private _translateService: TranslateService,
    private _displayService: DisplayService,
    private _adminRequirementService: AdminRequirementService
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

  public findRequirementDescription(code: string): Observable<string> {
    return this._adminRequirementService.findRequirementDescription(code);
  }

  public save(requirement: RequirementInformationWithTranslation): void {
    this.service.saveRequirement(this.entityId, requirement).subscribe(saved => {
      this.requirements.push(this._adminRequirementService.handleRequirementTranslation(saved));
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
      this.requirements = requirements.map(current => this._adminRequirementService.handleRequirementTranslation(current))
    );
  }
}
