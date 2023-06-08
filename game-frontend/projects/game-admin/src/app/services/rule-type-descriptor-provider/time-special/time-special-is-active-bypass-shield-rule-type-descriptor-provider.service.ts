import { Injectable } from '@angular/core';
import { RuleTypeDescriptorProviderService } from '../rule-type-descriptor-provider-service.interface';
import {Observable, of} from 'rxjs';
import { RuleTypeDescriptor } from '../../../types/rule-type-descriptor.type';

@Injectable()
export class TimeSpecialIsActiveBypassShieldRuleTypeDescriptorProviderService implements RuleTypeDescriptorProviderService {
    findRuleTypeDescriptor(): Observable<RuleTypeDescriptor> {
        return of({
            name: 'TIME_SPECIAL_IS_ENABLED_BYPASS_SHIELD',
            allowedOrigins: ['TIME_SPECIAL'],
            extraArgs: []
        });
    }
}
