import { ReplaySubject, Subject } from 'rxjs';
import { Rule } from '../types/rule.type';

export class RuleStore {
    public readonly rules: Subject<Rule[]> = new ReplaySubject(1);
}
