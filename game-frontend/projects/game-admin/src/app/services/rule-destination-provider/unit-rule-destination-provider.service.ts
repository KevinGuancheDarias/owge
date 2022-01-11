import { Injectable } from '@angular/core';
import { IdName } from '@owge/core';
import { Observable } from 'rxjs';
import { AdminUnitService } from '../admin-unit.service';
import { RuleDestinationProviderService } from './rule-destination-provider-service.interface';

@Injectable()
export class UnitRuleDestinationProviderService implements RuleDestinationProviderService {

    constructor(private adminUnitService: AdminUnitService) {

    }

    findById(id: number): Observable<IdName> {
        return this.adminUnitService.findOneById(id);
    }

    findElements(): Observable<IdName[]> {
        return this.adminUnitService.findAll();
    }

    getDestinationProviderId(): string {
        return 'UNIT';
    }
}
