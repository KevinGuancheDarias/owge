import { Component, Input, OnChanges, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractModalContainerComponent, IdName, ModalComponent } from '@owge/core';
import { Observable, Subscription } from 'rxjs';
import { AdminRuleService } from '../../services/admin-rule.service';
import { RuleTypeDescriptor } from '../../types/rule-type-descriptor.type';
import { Rule } from '../../types/rule.type';

@Component({
  selector: 'app-rules-modal',
  templateUrl: './rules-modal.component.html',
  styleUrls: ['./rules-modal.component.scss']
})
export class RulesModalComponent extends AbstractModalContainerComponent implements OnInit, OnChanges, OnDestroy {

  @Input() originType: string;
  @Input() originId: number;
  @Input() displayName: string;

  @ViewChild('crudModal') private crudModal: ModalComponent;

  rules$: Observable<Rule[]>;
  ruleTypes: RuleTypeDescriptor[];
  destinationTypes: string[];
  destinationItems$: Observable<IdName[]>;
  editingRule: Partial<Rule>;
  editingRuleTypeDescriptor: RuleTypeDescriptor;

  private ruleTypesSubscription: Subscription;

  constructor(
    private adminRuleService: AdminRuleService,
    private translateService: TranslateService
  ) {
    super();
  }

  ngOnInit(): void {
    this.ruleTypesSubscription = this.adminRuleService.findAvailableTypes().subscribe(types => this.ruleTypes = types);
    this.destinationTypes = this.adminRuleService.findDestinationTypes();
  }

  ngOnChanges(): void {
    this.loadRules();
  }

  ngOnDestroy(): void {
    this.ruleTypesSubscription.unsubscribe();
  }

  clickEdit(rule: Rule): void {
    this.editingRule = { ...rule };
    this.onTypeChange();
    this.onDestinationTypeChange();
    this.editingRule.destinationId = rule.destinationId;
    this.crudModal.show();
  }

  clickNew(): void {
    this.editingRule = {
      originId: this.originId,
      originType: this.originType
    };
    this.crudModal.show();
  }

  clickDelete(rule: Rule): void {
    this.adminRuleService.deleteById(rule.id).subscribe(() => this.loadRules());
  }

  async clickSave(): Promise<void> {
    if (!this.editingRule.type) {
      alert(this.translateService.instant('APP.RULES.CRUD_MODAL.MISSING_TYPE'));
    } else {
      await this.adminRuleService.save(this.editingRule).toPromise();
      this.loadRules();
      this.crudModal.hide();
    }
  }

  async clickCancel(): Promise<void> {
    delete this.editingRule;
    this.crudModal.hide();
  }

  onDestinationTypeChange(): void {
    delete this.editingRule.destinationId;
    if (this.editingRule.destinationType) {
      this.destinationItems$ = this.adminRuleService.findElementsByDestinationProviderId(this.editingRule.destinationType);
    } else {
      delete this.destinationItems$;
    }
  }

  onTypeChange(): void {
    this.editingRuleTypeDescriptor = this.ruleTypes.find(type => type.name === this.editingRule.type);
    this.editingRule.extraArgs = this.editingRule.extraArgs
      ? this.editingRule.extraArgs
      : new Array(this.editingRuleTypeDescriptor.extraArgs.length);
  }

  private loadRules(): void {
    this.rules$ = this.adminRuleService.findByOriginTypeAndOriginId(this.originType, this.originId);
  }

}
