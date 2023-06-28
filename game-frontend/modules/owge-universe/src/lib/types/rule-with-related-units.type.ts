import {Rule} from '@owge/universe';
import {CommonEntity} from '@owge/core';

export interface RuleWithRelatedUnits {
    rules: Rule[];
    relatedUnits: {[key: string]: CommonEntity};
}
