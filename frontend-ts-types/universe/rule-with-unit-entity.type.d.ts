import {Rule} from './rule.type';
import {CommonEntity} from '../core';

export interface RuleWithUnitEntity extends Rule{
    unitEntity: CommonEntity;
}
