import { ReplaySubject, Subject } from 'rxjs';
import { Rule } from '@owge/types/universe';
import {CommonEntity} from '@owge/types/core';

export class RuleStore {
    public readonly rules: Subject<Rule[]> = new ReplaySubject(1);
    public readonly relatedUnits: Subject<{[key: string]: CommonEntity}> = new ReplaySubject(1);
}
