import { Component, Input, OnChanges, ViewChild } from '@angular/core';
import { WithRequirementsCrudMixin } from '@owge/universe';
import { LoadingService, ModalComponent, RequirementInformation, RequirementGroup } from '@owge/core';
import { RequirementInformationWithTranslation } from '../../types/requirement-information-with-translation.type';
import { RequirementsModalComponent } from '../requirements-modal/requirements-modal.component';
import { TranslateService } from '@ngx-translate/core';
import { take } from 'rxjs/operators';
import { WidgetConfirmationDialogComponent } from '@owge/widgets';
import { AdminRequirementService } from '../../services/admin-requirement.service';

@Component({
  selector: 'app-object-requirement-groups-crud',
  templateUrl: './object-requirement-groups-crud.component.html',
  styleUrls: ['./object-requirement-groups-crud.component.scss']
})
export class ObjectRequirementGroupsCrudComponent implements OnChanges {

  @Input() public entityId: any;
  @Input() public service: WithRequirementsCrudMixin<any>;
  @ViewChild(ModalComponent) public addGroupModal: ModalComponent;
  @ViewChild(RequirementsModalComponent) public requirementsModal: RequirementsModalComponent;

  public requirementGroups: RequirementGroup[];

  public newGroup: RequirementGroup;
  public newRequirement: RequirementInformationWithTranslation;
  public deleteGroupConfirmText: string;

  @ViewChild(WidgetConfirmationDialogComponent) private _confirmDialog: WidgetConfirmationDialogComponent;
  private _targetGroup: RequirementGroup;
  private _groupToDelete: RequirementGroup;

  constructor(
    private _loadingService: LoadingService,
    private _translateService: TranslateService,
    private _adminRequirementService: AdminRequirementService
  ) { }

  public ngOnChanges() {
    if (this.entityId) {
      this._loadRequirements();
    }
  }

  public add(group: RequirementGroup): void {
    this._targetGroup = group;
    this.newRequirement = <any>{ requirement: {} };
    this.requirementsModal.show();
  }

  public clickSave(): void {
    this._loadingService.runWithLoading(async () => {
      await this.service.addGroup(this.entityId, this.newGroup.name).toPromise();
      await this._loadRequirements();
      this.addGroupModal.hide();
    });
  }

  public async clickDeleteGroup(group: RequirementGroup): Promise<void> {
    this.deleteGroupConfirmText = await this._translateService.get('CRUD.CONFIRM_DELETE', { elName: group.name }).pipe(take(1)).toPromise();
    this._groupToDelete = group;
    this._confirmDialog.show();
  }

  public confirmDelete(confirm: boolean): void {
    if (confirm) {
      this._loadingService.runWithLoading(async () => {
        await this.service.deleteRequirementGroup(this.entityId, this._groupToDelete.id).toPromise();
        await this._loadRequirements();
      });
    }
    this._confirmDialog.hide();
  }

  public save(requirement: RequirementInformation): void {
    this._loadingService.runWithLoading(async () => {
      await this.service.addRequirementToGroup(this.entityId, this._targetGroup.id, requirement).toPromise();
      await this._loadRequirements();
      this.requirementsModal.hide();
    });
  }

  public delete(group: RequirementGroup, requirement: RequirementInformation): void {
    this._loadingService.runWithLoading(async () => {
      await this.service.deleteRequirementByGroupAndId(this.entityId, group.id, requirement.id).toPromise();
      await this._loadRequirements();
    });
  }

  private async _loadRequirements(): Promise<void> {
    this.requirementGroups = await this._loadingService.addPromise(
      this.service.findRequirementGroups(this.entityId).toPromise()
    );
    this.requirementGroups.forEach(group =>
      group.requirements = group.requirements.map(requirement => this._adminRequirementService.handleRequirementTranslation(requirement))
    );
  }

}
