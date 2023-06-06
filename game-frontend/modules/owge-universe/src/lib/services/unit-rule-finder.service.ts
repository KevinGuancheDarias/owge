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

    findUnitRule(ruleType: string, from: Unit, to: Unit): Observable<Rule> {
        return combineLatest([
            this.ruleService.findAll(),
            this.unitTypeService.getUnitTypes()
        ]).pipe(
            map(rules => this.doFindRule(rules[0].filter(rule => rule.type === ruleType), rules[1], from, to)),
        );
    }

    private doFindRule(rules: Rule[], unitTypes: UnitType[], from: Unit, to: Unit): Rule {
        console.log('Finding!!!', rules, unitTypes);
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
