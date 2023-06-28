import { ReplaySubject, Subject } from 'rxjs';
import { Rule } from '../types/rule.type';
import {CommonEntity} from '@owge/core';

export class RuleStore {
    public readonly rules: Subject<Rule[]> = new ReplaySubject(1);
    public readonly relatedUnits: Subject<{[key: string]: CommonEntity}> = new ReplaySubject(1);
}
