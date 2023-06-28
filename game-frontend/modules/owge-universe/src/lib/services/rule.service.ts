import { Injectable } from '@angular/core';
import {AbstractWebsocketApplicationHandler, CommonEntity} from '@owge/core';
import { Observable } from 'rxjs';
import { RuleStore } from '../storages/rule.store';
import { Rule } from '../types/rule.type';
import {RuleWithRelatedUnits} from '../types/rule-with-related-units.type';
import {map, take} from 'rxjs/operators';

@Injectable()
export class RuleService extends AbstractWebsocketApplicationHandler{
    private ruleStore: RuleStore = new RuleStore;

    constructor() {
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

    findByOriginTypeAndOriginId(originType: 'TIME_SPECIAL' | 'UNIT', originId: number): Observable<Rule[]> {
        return this.findAll()
            .pipe(
                map(rules => rules.filter(rule => rule.originType === originType && rule.originId === originId ))
            );
    }

    isWantedType(rule: Rule, type: string): boolean {
        console.warn('bar',rule, type);
        return type === rule.type;
    }

    findRelatedUnit(id: number): Promise<CommonEntity>|Promise<null> {
        return this.findRelatedUnits().pipe(
            map(units => units[id]),
            take(1)
        ).toPromise();
    }

    _onRuleChange(content: RuleWithRelatedUnits): void {
        this.ruleStore.rules.next(content.rules);
        this.ruleStore.relatedUnits.next(content.relatedUnits);
    }
}
