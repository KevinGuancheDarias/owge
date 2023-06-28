import { Injectable } from '@angular/core';
import { UnitType } from '@owge/core';
import { combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Rule } from '../types/rule.type';
import { Unit } from '../types/unit.type';
import { RuleService } from './rule.service';
import { UnitTypeService } from './unit-type.service';

@Injectable()
export class UnitRuleFinderService {
    private static readonly originUnit = 'UNIT';
    private static readonly originUnitType = 'UNIT_TYPE';

    constructor(private ruleService: RuleService, private unitTypeService: UnitTypeService) {}

    findUnitRuleFromTo(ruleType: string, from: Unit, to: Unit): Observable<Rule> {
        return this.doCombine().pipe(
            map(combination => this.handleCombination(
                combination,
                ruleType,
                from,
                to,
                this.doFindRuleFromTo.bind(this))
            )
        );
    }

    findRulesForUnit(ruleType: string, from: Unit): Observable<Rule[]> {
        return this.doCombine().pipe(
            map(combination => this.handleCombination(combination, ruleType, from, null, this.doFindRulesForUnit.bind(this)))
        );
    }

    private doCombine(): Observable<[Rule[], UnitType[]]> {
        return combineLatest([
            this.ruleService.findAll(),
            this.unitTypeService.getUnitTypes()
        ]);
    }

    private handleCombination<T>(
        combination: [Rule[],UnitType[]],
        ruleType: string,
        from: Unit,
        to: Unit|null,
        resolver: (rulesOfType: Rule[], unitTypes: UnitType[], from: Unit, to?: Unit) => T
    ): T {
        const rulesOfType = combination[0].filter(rule => rule.type === ruleType);
        return resolver(rulesOfType, combination[1], from, to);
    }

    private doFindRulesForUnit(rulesOfType: Rule[], unitTypes: UnitType[], from: Unit): Rule[] {
        const fromUnitType: UnitType = this.findUnitTypeForUnit(unitTypes, from);
        return rulesOfType
            .filter(
                rule => rule.originType === UnitRuleFinderService.originUnit && rule.originId === from.id
                    || this.ruleAffectsSelfOrParentUnitType(rule, fromUnitType)
            );
    }

    private ruleAffectsSelfOrParentUnitType(rule: Rule, unitType: UnitType): boolean {
        if(rule.originType === UnitRuleFinderService.originUnitType && rule.originId === unitType.id) {
            return true;
        } else if(unitType.parent) {
            return this.ruleAffectsSelfOrParentUnitType(rule, unitType.parent);
        } else {
            return false;
        }
    }

    private doFindRuleFromTo(rules: Rule[], unitTypes: UnitType[], from: Unit, to: Unit): Rule {
        const fromUnitType: UnitType = this.findUnitTypeForUnit(unitTypes, from);
        const toUnitType: UnitType = this.findUnitTypeForUnit(unitTypes, to);
        return this.unitVsUnitFind(rules, from, to)
            || (toUnitType && this.unitVsUnitTypeFind(rules, from, toUnitType))
            || (fromUnitType && this.unitTypeVsUnit(rules, fromUnitType, to))
            || (fromUnitType && toUnitType && this.unitTypeVsUnitType(rules, fromUnitType, toUnitType))
            || null;
    }

    private unitVsUnitFind(rules: Rule[], from: Unit, to: Unit): Rule {
        return rules.find(rule =>
            rule.originType === UnitRuleFinderService.originUnit && rule.originId === from.id
                && rule.destinationType === UnitRuleFinderService.originUnit && rule.destinationId === to.id
        );
    }

    private unitVsUnitTypeFind(rules: Rule[], from: Unit, to: UnitType): Rule {
        const retVal: Rule = rules.find(rule =>
            rule.originType === UnitRuleFinderService.originUnit && rule.originId === from.id
                && rule.destinationType === UnitRuleFinderService.originUnitType && rule.destinationId === to.id
        );
        if(retVal !== null) {
            return retVal;
        } else if(to.parent) {
            return this.unitVsUnitTypeFind(rules, from, to.parent);
        }
        return null;
    }

    private unitTypeVsUnit(rules: Rule[], from: UnitType, to: Unit): Rule {
        const retVal: Rule = rules.find(rule =>
            rule.originType === UnitRuleFinderService.originUnitType && rule.originId === from.id
                && rule.destinationType === UnitRuleFinderService.originUnit && rule.destinationId === to.id
        );
        if(retVal != null ) {
            return retVal;
        } else if(from.parent) {
            return this.unitTypeVsUnit(rules, from.parent, to);
        }
        return null;
    }

    private unitTypeVsUnitType(rules: Rule[], from: UnitType, to: UnitType): Rule {
        const retVal: Rule = rules.find(rule =>
            rule.originType === UnitRuleFinderService.originUnitType && rule.originId === from.id
                && rule.destinationType === UnitRuleFinderService.originUnitType && rule.destinationId === to.id
        );
        if(retVal != null ) {
            return retVal;
        } else if(to.parent) {
            const toParentResult = this.unitTypeVsUnitType(rules, from, to.parent);
            if(toParentResult !== null) {
                return toParentResult;
            } else if(from.parent) {
                return this.unitTypeVsUnitType(rules, from.parent, to.parent);
            }
        }
        return null;
    }

    private findUnitTypeForUnit(unitTypes: UnitType[], target: Unit): UnitType {
        return unitTypes.find(unitType => unitType.id === target.typeId);
    }
}
