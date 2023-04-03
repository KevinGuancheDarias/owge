import { Injectable } from '@angular/core';
import { AbstractWebsocketApplicationHandler } from '@owge/core';
import { Observable } from 'rxjs';
import { RuleStore } from '../storages/rule.store';
import { Rule } from '../types/rule.type';

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

    _onRuleChange(content: Rule[]): void {
        this.ruleStore.rules.next(content);
    }
}
