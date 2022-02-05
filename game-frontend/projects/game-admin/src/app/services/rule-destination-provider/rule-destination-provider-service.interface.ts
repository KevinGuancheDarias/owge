import { InjectionToken } from '@angular/core';
import { IdName } from '@owge/core';
import { Observable } from 'rxjs';

export const ruleDestinationProviderServiceToken = new InjectionToken<RuleDestinationProviderService>('RuleDestinationProviderService');

export interface RuleDestinationProviderService {
    findById(id: number): Observable<IdName>;
    findElements(): Observable<IdName[]>;
    getDestinationProviderId(): string;
}
