import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { RuleTypeDescriptor } from '../../types/rule-type-descriptor.type';
import { RuleTypeDescriptorProviderService } from './rule-type-descriptor-provider-service.interface';

@Injectable()
export class TimeSpecialIsEnabledRuleTypeDescriptorProviderService implements RuleTypeDescriptorProviderService {

    findRuleTypeDescriptor(): Observable<RuleTypeDescriptor> {
        return of({
            name: 'TIME_SPECIAL_IS_ENABLED_DO_HIDE',
            allowedOrigins: ['TIME_SPECIAL'],
            extraArgs: []
        });
    }
}
