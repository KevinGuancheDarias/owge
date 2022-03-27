import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { RuleTypeDescriptor } from '../../../types/rule-type-descriptor.type';
import { RuleTypeDescriptorProviderService } from '../rule-type-descriptor-provider-service.interface';

@Injectable()
export class TimespecialIsActiveTemporalUnitsTypeDescriptorProviderService implements RuleTypeDescriptorProviderService {

    findRuleTypeDescriptor(): Observable<RuleTypeDescriptor> {
        return of({
            name: 'TIME_SPECIAL_IS_ENABLED_TEMPORAL_UNITS',
            allowedOrigins: ['TIME_SPECIAL'],
            allowedDestinations: ['UNIT'],
            extraArgs: [
                {
                    name: 'DURATION_IN_SECONDS',
                    formType: 'number'
                },
                {
                    name: 'UNIT_COUNT',
                    formType: 'number'
                }
            ]
        });
    }
}
