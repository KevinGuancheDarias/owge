import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractModalContainerComponent, CommonEntity, ProgrammingError, WidgetFilter } from '@owge/core';
import { WidgetFilterUtil } from '@owge/universe';
import { Observable } from 'rxjs';
import { AdminFactionService } from '../../services/admin-faction.service';
import { AdminSpecialLocationService } from '../../services/admin-special-location.service';
import { AdminUnitService } from '../../services/admin-unit.service';
import { AdminUpgradeService } from '../../services/admin-upgrade.service';
import { RequirementInformationWithTranslation } from '../../types/requirement-information-with-translation.type';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-requirements-modal',
  templateUrl: './requirements-modal.component.html',
  styleUrls: ['./requirements-modal.component.scss']
})
export class RequirementsModalComponent extends AbstractModalContainerComponent {

  @Input() public newRequirement: RequirementInformationWithTranslation;
  @Output() public save: EventEmitter<RequirementInformationWithTranslation> = new EventEmitter;

  public secondValueList: CommonEntity<number>[];

  public secondValueFilters: WidgetFilter<any>[] = null;
  public secondValueListFiltered: CommonEntity<number[]>;

  public allowedRequirements = ['BEEN_RACE', 'UPGRADE_LEVEL', 'HAVE_SPECIAL_LOCATION', 'HAVE_UNIT'];

  public constructor(
    private _translateService: TranslateService,
    private _adminFactionService: AdminFactionService,
    private _adminUpgradeService: AdminUpgradeService,
    private _adminSpecialLocationService: AdminSpecialLocationService,
    private _adminUnitService: AdminUnitService
  ) {
    super();
  }

  public loadSecondValue() {
    delete this.newRequirement.secondValue;
    delete this.secondValueFilters;
    if (this.newRequirement.requirement.code) {
      this.secondValueList = null;
      this.secondValueListFiltered = null;
      delete this.newRequirement.targetThirdValueTranslation;
      this._translateService.get(`REQUIREMENTS.TARGETS.${this.newRequirement.requirement.code}`)
        .subscribe(val => this.newRequirement.targetSecondValueTranslation = val);
      switch (this.newRequirement.requirement.code) {
        case 'BEEN_RACE':
          this._adminFactionService.findAll().subscribe(factions => this.secondValueList = factions);
          break;
        case 'UPGRADE_LEVEL':
          this._adminFactionService.buildFilter().then(result => this.secondValueFilters = [result]);
          this._adminUpgradeService.findAll().subscribe(upgrades => this.secondValueList = upgrades);
          this._translateService.get('REQUIREMENTS.THIRD_VALUES.UPGRADE_LEVEL')
            .subscribe(translation => this.newRequirement.targetThirdValueTranslation = translation);
          break;
        case 'HAVE_SPECIAL_LOCATION':
          this._adminSpecialLocationService.findAll().subscribe(specialLocations => this.secondValueList = specialLocations);
          break;
        case 'HAVE_UNIT':
          this._adminFactionService.buildFilter().then(result => this.secondValueFilters = [result, WidgetFilterUtil.buildByNameFilter()]);
          this._adminUnitService.findAll().subscribe(units => this.secondValueList = units);
          break;
        default:
          throw new ProgrammingError(`The requirement with code ${this.newRequirement.requirement.code} is not supported`);
      }
    } else {
      this.newRequirement.targetSecondValueTranslation = '';
    }
  }

  public findRequirementDescription(code: string): Observable<string> {
    return this._translateService.get(`REQUIREMENTS.DESCRIPTIONS.${code}`, {thirdValue: ''});
  }

  public cancel() {
    this._childModal.hide();
  }

  public canAdd(): boolean {
    return this.newRequirement.targetSecondValueTranslation &&
      !!((this.newRequirement.targetThirdValueTranslation && this.newRequirement.secondValue && this.newRequirement.thirdValue)
        || (!this.newRequirement.targetThirdValueTranslation && this.newRequirement.secondValue));
  }

  public clickSave(): void {
    const toSave: RequirementInformationWithTranslation = { ...this.newRequirement };
    delete toSave.targetSecondValueTranslation;
    delete toSave.targetThirdValueTranslation;
    this.save.emit(toSave);
  }
}
