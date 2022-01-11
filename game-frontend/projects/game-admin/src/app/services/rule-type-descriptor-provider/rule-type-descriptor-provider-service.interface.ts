import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { RuleTypeDescriptor } from '../../types/rule-type-descriptor.type';

export const ruleTypeDescriptorProviderToken = new InjectionToken<RuleTypeDescriptorProviderService>('RuleTypeDescriptorProviderService');

export interface RuleTypeDescriptorProviderService {
    findRuleTypeDescriptor(): Observable<RuleTypeDescriptor>;
}
