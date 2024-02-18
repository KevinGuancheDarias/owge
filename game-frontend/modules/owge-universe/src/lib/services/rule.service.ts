import { Injectable } from '@angular/core';
import {AbstractWebsocketApplicationHandler, AsyncCollectionUtil} from '@owge/core';
import { CommonEntity } from '@owge/types/core';
import { Observable } from 'rxjs';
import { RuleStore } from '../storages/rule.store';
import {map, take} from 'rxjs/operators';
import {UnitTypeService} from './unit-type.service';
import { Rule, RuleWithRelatedUnits, RuleWithUnitEntity } from '@owge/types/universe';

@Injectable()
export class RuleService extends AbstractWebsocketApplicationHandler{
    private ruleStore: RuleStore = new RuleStore;

    constructor(private unitTypeService: UnitTypeService) {
        super();
        this._eventsMap = {
            // eslint-disable-next-line @typescript-eslint/naming-convention
            rule_change: '_onRuleChange'
        };
    }

    findAll(): Observable<Rule[]> {
        return this.ruleStore.rules.asObservable();
    }

    findRelatedUnits(): Observable<{[key: string]: CommonEntity}> {
        return this.ruleStore.relatedUnits.asObservable();
    }

    findByOrigin(originType: 'TIME_SPECIAL' | 'UNIT'): Observable<Rule[]> {
        return this.findAll()
            .pipe(
                map(rules => rules.filter(rule => rule.originType === originType))
            );
    }

    findByOriginTypeAndOriginId(originType: 'TIME_SPECIAL' | 'UNIT', originId: number): Observable<Rule[]> {
        return this.findAll()
            .pipe(
                map(rules => rules.filter(rule => rule.originType === originType && rule.originId === originId ))
            );
    }

    isWantedType(rule: Rule, type: string): boolean {
        return type === rule.type;
    }

    findRelatedUnit(rule: Rule): Promise<CommonEntity>|Promise<null> {
        if(rule.destinationType === 'UNIT') {
            return this.findRelatedUnits().pipe(
                map(units => units[rule.destinationId]),
                take(1)
            ).toPromise();
        } else if(rule.destinationType === 'UNIT_TYPE') {
            return this.unitTypeService.idToUnitType(rule.destinationId);
        }
    }

    async addRelatedUnits(rules: Rule[]): Promise<RuleWithUnitEntity[]> {
        return await AsyncCollectionUtil.map(
            rules, async rule => ({...rule,unitEntity: await this.findRelatedUnit(rule)})
        );
    }

    _onRuleChange(content: RuleWithRelatedUnits): void {
        this.ruleStore.rules.next(content.rules);
        this.ruleStore.relatedUnits.next(content.relatedUnits);
    }
}
