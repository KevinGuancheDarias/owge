import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { RuleTypeDescriptor } from '../../../types/rule-type-descriptor.type';
import { RuleTypeDescriptorProviderService } from '../rule-type-descriptor-provider-service.interface';

@Injectable()
export class UnitStoresUnitRuleTypeDescriptorProviderService implements RuleTypeDescriptorProviderService {
    findRuleTypeDescriptor(): Observable<RuleTypeDescriptor> {
        return of({
            name: 'UNIT_STORES_UNIT',
            allowedOrigins: ['UNIT', 'UNIT_TYPE'],
            extraArgs: []
        });
    }

}
