import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { RuleTypeDescriptor } from '../../types/rule-type-descriptor.type';
import { RuleTypeDescriptorProviderService } from './rule-type-descriptor-provider-service.interface';

@Injectable()
export class UnitCaptureRuleTypeDescriptorProviderService implements RuleTypeDescriptorProviderService {

    findRuleTypeDescriptor(): Observable<RuleTypeDescriptor> {
        return of({
            name: 'UNIT_CAPTURE',
            allowedOrigins: ['UNIT'],
            extraArgs: [
                {
                    name: 'PROBABILITY_PERCENTAGE',
                    formType: 'number'
                },
                {
                    name: 'CAPTURE_PERCENTAGE',
                    formType: 'number'
                }
            ]
        });
    }
}
