import { Inject, Injectable } from '@angular/core';
import { IdName, ProgrammingError } from '@owge/core';
import { UniverseGameService, Rule } from '@owge/universe';
import { combineLatest, Observable } from 'rxjs';
import { map, take, tap } from 'rxjs/operators';
import { RuleTypeDescriptor } from '../types/rule-type-descriptor.type';
import {
    RuleDestinationProviderService,
    ruleDestinationProviderServiceToken
} from './rule-destination-provider/rule-destination-provider-service.interface';
import {
    RuleTypeDescriptorProviderService,
    ruleTypeDescriptorProviderToken
} from './rule-type-descriptor-provider/rule-type-descriptor-provider-service.interface';

@Injectable()
export class AdminRuleService {
    constructor(
        @Inject(ruleDestinationProviderServiceToken) protected ruleDestinationProviderServices: RuleDestinationProviderService[],
        @Inject(ruleTypeDescriptorProviderToken) protected ruleTypeDescriptorProviderServices: RuleTypeDescriptorProviderService[],
        protected universeGameService: UniverseGameService
    ) { }

    deleteById(id: number): Observable<void> {
        return this.universeGameService.requestWithAutorizationToContext('admin', 'delete', `rules/${id}`).pipe(take(1));
    }

    findAvailableTypes(): Observable<RuleTypeDescriptor[]> {
        return combineLatest(this.ruleTypeDescriptorProviderServices.map(service => service.findRuleTypeDescriptor()));
    }

    findByOriginTypeAndOriginId(originType: string, originId: number): Observable<Rule[]> {
        return this.universeGameService.requestWithAutorizationToContext<Rule[]>(
            'admin',
            'get',
            `rules/origin/${originType}/${originId}`
        ).pipe(
            tap(rules => rules.map(rule => this.addDestinationNameResolverToRule(rule)))
        );
    }

    findDestinationTypes(): string[] {
        return this.ruleDestinationProviderServices.map(service => service.getDestinationProviderId());
    }

    findElementsByDestinationProviderId(provider: string): Observable<IdName[]> {
        return this.findDestinationProvider(provider).findElements();
    }

    save(rule: Partial<Rule>): Observable<Rule> {
        return this.universeGameService.requestWithAutorizationToContext('admin', 'post', 'rules', { ...rule, destinationName$: null });
    }

    private findDestinationProvider(provider: string): RuleDestinationProviderService {
        const retVal = this.ruleDestinationProviderServices.find(service => service.getDestinationProviderId() === provider);
        if (!retVal) {
            throw new ProgrammingError(`Rule destination provider ${provider} is not valid, valid ares ${this.findDestinationTypes()}`);
        }
        return retVal;
    }

    private addDestinationNameResolverToRule(rule: Rule): void {
        rule.destinationName$ = this.findDestinationProvider(rule.destinationType).findById(rule.destinationId)
            .pipe(map(item => item.name));
    }
}
