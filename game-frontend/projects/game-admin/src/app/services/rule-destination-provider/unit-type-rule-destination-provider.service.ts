import { Injectable } from '@angular/core';
import { IdName } from '@owge/core';
import { Observable } from 'rxjs';
import { AdminUnitTypeService } from '../admin-unit-type.service';
import { RuleDestinationProviderService } from './rule-destination-provider-service.interface';

@Injectable()
export class UnitTypeRuleDestinationProviderService implements RuleDestinationProviderService {

    constructor(private adminUnitTypeService: AdminUnitTypeService) {

    }

    findById(id: number): Observable<IdName> {
        return this.adminUnitTypeService.findOneById(id);
    }

    findElements(): Observable<IdName[]> {
        return this.adminUnitTypeService.findAll();
    }

    getDestinationProviderId(): string {
        return 'UNIT_TYPE';
    }
}
