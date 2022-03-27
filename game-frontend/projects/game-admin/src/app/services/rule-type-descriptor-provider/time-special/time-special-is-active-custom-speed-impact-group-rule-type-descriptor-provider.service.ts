import { Injectable } from '@angular/core';
import { IdName } from '@owge/core';
import { Observable, of } from 'rxjs';
import { RuleTypeDescriptor } from '../../../types/rule-type-descriptor.type';
import { AdminSpeedImpactGroupService } from '../../admin-speed-impact-group.service';
import { RuleTypeDescriptorProviderService } from '../rule-type-descriptor-provider-service.interface';

@Injectable()
export class TimeSpecialIsActiveCustomSpeedImpactGroupRuleTypeDescriptorProviderService implements RuleTypeDescriptorProviderService {

    constructor(private adminSpeedImpactGroupService: AdminSpeedImpactGroupService) {

    }

    findRuleTypeDescriptor(): Observable<RuleTypeDescriptor> {
        const $this = this;
        return of({
            name: 'TIME_SPECIAL_IS_ENABLED_DO_SWAP_SPEED_IMPACT_GROUP',
            allowedOrigins: ['TIME_SPECIAL'],
            extraArgs: [{
                name: 'SPEED_IMPACT_GROUP',
                formType: 'select',
                data$: this.adminSpeedImpactGroupService.findAll(),
                comparatorFn: (a, b) => {
                    const retVal = $this.toId(a) === $this.toId(b);
                    return retVal;
                }
            }]
        });
    }

    private toId(entry: IdName | number | string): number {
        if (typeof entry === 'number' || typeof entry === 'string') {
            return +entry;
        } else if (entry?.id) {
            return +entry.id;
        } else {
            return null;
        }
    }
}
