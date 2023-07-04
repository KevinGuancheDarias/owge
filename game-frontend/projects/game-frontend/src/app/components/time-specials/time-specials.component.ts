import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {DateRepresentation, DateUtil, ModalComponent, ObservableSubscriptionsHelper} from '@owge/core';
import { UserWithFaction } from '@owge/faction';
import {
  TimeSpecial,
  TimeSpecialService,
  RuleService,
  RuleWithUnitEntity,
  SpeedImpactGroupService, Rule
} from '@owge/universe';
import { BaseComponent } from '../../base/base.component';
import {combineLatest} from 'rxjs';

/**
 * Component to display and handle the time specials
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Component({
  selector: 'app-time-specials',
  templateUrl: './time-specials.component.html',
  styleUrls: ['./time-specials.component.less'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TimeSpecialsComponent extends BaseComponent<UserWithFaction> implements OnInit, OnDestroy {
  @ViewChild(ModalComponent) modal: ModalComponent;

  elements: TimeSpecial[];
  selectedElement: TimeSpecial;
  rulesForTimeSpecial: RuleWithUnitEntity[][] = [];
  rulesForCapturingUnits: RuleWithUnitEntity[];
  rulesForBypassShield: RuleWithUnitEntity[];
  rulesForTemporalUnits: RuleWithUnitEntity[];
  rulesForHiddenUnits: RuleWithUnitEntity[];
  rulesThatAlterSpeedGroup: RuleWithUnitEntity[];

  private additionalObservableSubscriptions: ObservableSubscriptionsHelper = new ObservableSubscriptionsHelper();

  constructor(
    private _timeSpecialService: TimeSpecialService,
    private ruleService: RuleService,
    private speedImpactGroupService: SpeedImpactGroupService,
    private _cdr: ChangeDetectorRef
  ) {
    super();
  }

  ngOnInit() {
    this.requireUser();
    this._subscriptions.add(
        combineLatest([this._timeSpecialService.findUnlocked(), this.ruleService.findByOrigin('TIME_SPECIAL')])
            .subscribe(async elements => {
              const rules = await this.ruleService.addRelatedUnits(elements[1]);
              this.elements = elements[0];
              this.elements.forEach((element, index) =>
                  this.rulesForTimeSpecial[index] = rules.filter(rule => rule.originId === element.id)
              );
              this._cdr.detectChanges();
            })
    );
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.additionalObservableSubscriptions.unsubscribeAll();
  }

  showInfo(i: number) {
    this.additionalObservableSubscriptions.unsubscribeAll();
    this.selectedElement = this.elements[i];
    const timeSpecialRules = this.rulesForTimeSpecial[i];
    this.rulesForCapturingUnits = timeSpecialRules.filter(rule => rule.type === 'UNIT_CAPTURE');
    this.rulesForTemporalUnits = timeSpecialRules.filter(rule => rule.type === 'TIME_SPECIAL_IS_ENABLED_TEMPORAL_UNITS');
    this.rulesForHiddenUnits = timeSpecialRules.filter(rule => rule.type === 'TIME_SPECIAL_IS_ENABLED_DO_HIDE');
    this.rulesThatAlterSpeedGroup = timeSpecialRules.filter(rule => rule.type === 'TIME_SPECIAL_IS_ENABLED_DO_SWAP_SPEED_IMPACT_GROUP');
    this.rulesThatAlterSpeedGroup.forEach(rule => this.solveSpeedImpactGroup(rule));
    this.rulesForBypassShield = timeSpecialRules.filter(rule => rule.type === 'TIME_SPECIAL_IS_ENABLED_BYPASS_SHIELD');
    this._cdr.detectChanges();
    this.modal.show();
  }

  /**
   * Activates the time special
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @param timeSpecialId
   */
  clickActivate(timeSpecialId: number): void {
    this._doWithLoading(this._timeSpecialService.activate(timeSpecialId).toPromise());
  }

  parsedRequiredTime(timeInSeconds: number): DateRepresentation {
    return DateUtil.milisToDaysHoursMinutesSeconds(timeInSeconds * 1000);
  }

  solveSpeedImpactGroup(rule: Rule): void {
    const magicRule = rule as any;
    this.additionalObservableSubscriptions.add(
      this.speedImpactGroupService.findById(Number(rule.extraArgs[0])).subscribe(entity => {
        magicRule.targetName = entity.name;
      })
    );
  }
}
