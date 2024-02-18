import {Rule} from '../universe';
import {CommonEntity} from '../core';

export interface RuleWithRelatedUnits {
    rules: Rule[];
    relatedUnits: {[key: string]: CommonEntity};
}
