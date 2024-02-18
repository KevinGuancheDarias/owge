import {Injectable} from '@angular/core';
import {ActiveTimeSpecialService} from './active-time-special.service';
import {Observable, of} from 'rxjs';
import { Rule } from '@owge/types/universe';
import {RuleService} from './rule.service';
import {map, mergeAll, mergeMap} from 'rxjs/operators';

@Injectable()
export class ActiveTimeSpecialRuleFinderService {
    constructor(
        private activeTimeSpecialService: ActiveTimeSpecialService,
        private ruleService: RuleService
    ) {}

    findActiveRules(wantedType: string, additionalFilter: (rule: Rule) => boolean = () => true): Observable<Rule[]> {
        return this.activeTimeSpecialService.findByStatus('ACTIVE')
            .pipe(
                mergeMap(activeTimeSpecials => activeTimeSpecials.length
                    ? activeTimeSpecials.map(activeTimeSpecial =>
                        this.ruleService.findByOriginTypeAndOriginId('TIME_SPECIAL', activeTimeSpecial.timeSpecial)
                    )
                    : [of([])]
                ),
                mergeAll(),
                map(rules => rules.filter(rule => this.ruleService.isWantedType(rule, wantedType) && additionalFilter(rule)))
            );
    }
}
