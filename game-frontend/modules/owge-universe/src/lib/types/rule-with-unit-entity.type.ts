import {Rule} from './rule.type';
import {CommonEntity} from '@owge/core';

export interface RuleWithUnitEntity extends Rule{
    unitEntity: CommonEntity;
}
