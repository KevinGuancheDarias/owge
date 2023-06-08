import { HttpClient, HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ServiceWorkerModule } from '@angular/service-worker';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import {
  CoreModule, JwtTokenUtil, LoadingService, OwgeUserModule, SessionService, ThemeService
} from '@owge/core';
import { OwgeUniverseModule } from '@owge/universe';
import { OwgeWidgetsModule } from '@owge/widgets';
import { environment } from '../environments/environment';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AdminUsersComponent } from './components/admin-users/admin-users.component';
import { AttackRuleCrudComponent } from './components/attack-rule-crud/attack-rule-crud.component';
import { CanDoMissionsCrudComponent } from './components/can-do-missions-crud/can-do-missions-crud.component';
import { CommonCrudWithImageComponent } from './components/common-crud-with-image/common-crud-with-image.component';
import { CommonCrudComponent } from './components/common-crud/common-crud.component';
import { ConfigurationCrudComponent } from './components/configuration-crud/configuration-crud.component';
import { CriticalAttackCrudComponent } from './components/critical-attck-crud/critical-attack-crud.component';
import { FactionCrudComponent } from './components/faction-crud/faction-crud.component';
import { GalaxiesCrudComponent } from './components/galaxies-crud/galaxies-crud.component';
import { IndexComponent } from './components/index/index.component';
import { LoginComponent } from './components/login/login.component';
import { ObjectImprovementsCrudComponent } from './components/object-improvements-crud/object-improvements-crud.component';
import { ObjectRequirementGroupsCrudComponent } from './components/object-requirement-groups-crud/object-requirement-groups-crud.component';
import { ObjectRequirementsCrudComponent } from './components/object-requirements-crud/object-requirements-crud.component';
import { RequirementsFilterComponent } from './components/requirements-filter/requirements-filter.component';
import { RequirementsModalComponent } from './components/requirements-modal/requirements-modal.component';
import { RequirementsTableComponent } from './components/requirements-table/requirements-table.component';
import { ResourceRequirementsCrudComponent } from './components/resource-requirements-crud/resource-requirements-crud.component';
import { RulesModalComponent } from './components/rules-modal/rules-modal.component';
import { SpecialLocationCrudComponent } from './components/special-location-crud/special-location-crud.component';
import { SpeedImpactGroupCrudComponent } from './components/speed-impact-group-crud/speed-impact-group-crud.component';
import { TimeSpecialCrudComponent } from './components/time-special-crud/time-special-crud.component';
import { TranslatableComponent } from './components/translatable/translatable.component';
import { TutorialComponent } from './components/tutorial/tutorial.component';
import { UnitCrudComponent } from './components/unit-crud/unit-crud.component';
import { UnitTypeCrudComponent } from './components/unit-type-crud/unit-type-crud.component';
import { UpgradeCrudComponent } from './components/upgrade-crud/upgrade-crud.component';
import { UpgradeTypeCrudComponent } from './components/upgrade-type-crud/upgrade-type-crud.component';
import { AdminAttackRuleService } from './services/admin-attack-rule.service';
import { AdminConfigurationService } from './services/admin-configuration.service';
import { AdminCriticalAttackService } from './services/admin-critical-attack.service';
import { AdminFactionService } from './services/admin-faction.service';
import { AdminGalaxyService } from './services/admin-galaxy.service';
import { AdminLoginService } from './services/admin-login.service';
import { AdminRequirementService } from './services/admin-requirement.service';
import { AdminRuleService } from './services/admin-rule.service';
import { AdminSpecialLocationService } from './services/admin-special-location.service';
import { AdminSpeedImpactGroupService } from './services/admin-speed-impact-group.service';
import { AdminSystemMessageService } from './services/admin-system-message.service';
import { AdminTimeSpecialService } from './services/admin-time-special.service';
import { AdminTranslatableService } from './services/admin-translatable.service';
import { AdminTutorialEntryService } from './services/admin-tutorial-entry.service';
import { AdminTutorialService } from './services/admin-tutorial.service';
import { AdminUnitTypeService } from './services/admin-unit-type.service';
import { AdminUnitService } from './services/admin-unit.service';
import { AdminUpgradeTypeService } from './services/admin-upgrade-type.service';
import { AdminUpgradeService } from './services/admin-upgrade.service';
import { AdminUserService } from './services/admin-user.service';
import { ruleDestinationProviderServiceToken } from './services/rule-destination-provider/rule-destination-provider-service.interface';
import { UnitRuleDestinationProviderService } from './services/rule-destination-provider/unit-rule-destination-provider.service';
import { UnitTypeRuleDestinationProviderService } from './services/rule-destination-provider/unit-type-rule-destination-provider.service';
import { ruleTypeDescriptorProviderToken } from './services/rule-type-descriptor-provider/rule-type-descriptor-provider-service.interface';
import {
  TimeSpecialIsEnabledRuleTypeDescriptorProviderService
} from './services/rule-type-descriptor-provider/time-special-is-enabled-rule-type-descriptor-provider.service';
import {
  TimeSpecialIsActiveCustomSpeedImpactGroupRuleTypeDescriptorProviderService
  // eslint-disable-next-line max-len
} from './services/rule-type-descriptor-provider/time-special/time-special-is-active-custom-speed-impact-group-rule-type-descriptor-provider.service';
// eslint-disable-next-line max-len
import { TimespecialIsActiveTemporalUnitsTypeDescriptorProviderService } from './services/rule-type-descriptor-provider/time-special/time-special-is-active-temporal-units-rule-type-descriptor-provider.service';
import {
  UnitCaptureRuleTypeDescriptorProviderService
} from './services/rule-type-descriptor-provider/unit-capture-rule-type-descriptor-provider.service';
import {
  UnitStoresUnitRuleTypeDescriptorProviderService
} from './services/rule-type-descriptor-provider/unit/unit-stores-unit-rule-type-descriptor-provider.service';
import { UserListComponent } from './components/user-list/user-list.component';
import { UserDisplayComponent } from './components/user-display/user-display.component';
import { AdminGameUserService } from './services/admin-game-user.service';
import { SuspicionListComponent } from './components/suspicion-list/suspicion-list.component';
import { DisplaySuspicionComponent } from './components/display-suspicion/display-suspicion.component';
import { AdminSuspicionsService } from './services/admin-suspicions.service';
import {
  TimeSpecialIsActiveBypassShieldRuleTypeDescriptorProviderService
} from "./services/rule-type-descriptor-provider/time-special/time-special-is-active-bypass-shield-rule-type-descriptor-provider.service";

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    IndexComponent,
    CommonCrudComponent,
    TimeSpecialCrudComponent,
    CommonCrudWithImageComponent,
    ObjectRequirementsCrudComponent,
    ObjectImprovementsCrudComponent,
    UpgradeTypeCrudComponent,
    UnitTypeCrudComponent,
    FactionCrudComponent,
    ConfigurationCrudComponent,
    GalaxiesCrudComponent,
    UpgradeCrudComponent,
    ResourceRequirementsCrudComponent,
    UnitCrudComponent,
    SpecialLocationCrudComponent,
    RequirementsFilterComponent,
    AdminUsersComponent,
    ObjectRequirementGroupsCrudComponent,
    RequirementsTableComponent,
    RequirementsModalComponent,
    SpeedImpactGroupCrudComponent,
    CanDoMissionsCrudComponent,
    AttackRuleCrudComponent,
    TutorialComponent,
    TranslatableComponent,
    CriticalAttackCrudComponent,
    RulesModalComponent,
    UserListComponent,
    UserDisplayComponent,
    SuspicionListComponent,
    DisplaySuspicionComponent,
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    FormsModule,
    HttpClientModule,
    TranslateModule.forRoot({
      useDefaultLang: true,
      loader: {
        provide: TranslateLoader,
        useFactory: findHttpLoaderFactory,
        deps: [HttpClient]
      }
    }),
    CoreModule.forRoot({
      url: environment.accountUrl,
      loginEndpoint: environment.loginEndpoint,
      loginDomain: environment.loginDomain,
      loginClientId: environment.loginClientId,
      loginClientSecret: environment.loginClientSecret,
      contextPath: 'admin'
    }),
    ServiceWorkerModule.register('ngsw-worker.js', { enabled: false }),
    OwgeUserModule,
    OwgeUniverseModule.forRoot(),
    OwgeWidgetsModule,
  ],
  providers: [
    AdminLoginService,
    AdminTimeSpecialService,
    AdminFactionService,
    AdminUpgradeService,
    AdminUnitTypeService,
    AdminUpgradeTypeService,
    AdminConfigurationService,
    AdminGalaxyService,
    AdminUnitService,
    AdminSpecialLocationService,
    AdminUserService,
    AdminRequirementService,
    AdminSpeedImpactGroupService,
    AdminAttackRuleService,
    AdminCriticalAttackService,
    AdminTutorialService,
    AdminTutorialEntryService,
    AdminTranslatableService,
    AdminSystemMessageService,
    AdminRuleService,
    LoadingService,
    AdminGameUserService,
    AdminSuspicionsService,
    { provide: 'APPLICATION_CONTEXT', useValue: 'admin' },
    { provide: ruleDestinationProviderServiceToken, useClass: UnitRuleDestinationProviderService, multi: true },
    { provide: ruleDestinationProviderServiceToken, useClass: UnitTypeRuleDestinationProviderService, multi: true },
    { provide: ruleTypeDescriptorProviderToken, useClass: UnitCaptureRuleTypeDescriptorProviderService, multi: true },
    { provide: ruleTypeDescriptorProviderToken, useClass: TimeSpecialIsEnabledRuleTypeDescriptorProviderService, multi: true },
    { provide: ruleTypeDescriptorProviderToken, useClass: TimeSpecialIsActiveBypassShieldRuleTypeDescriptorProviderService, multi: true },
    {
      provide: ruleTypeDescriptorProviderToken,
      useClass: TimeSpecialIsActiveCustomSpeedImpactGroupRuleTypeDescriptorProviderService,
      multi: true
    },
    { provide: ruleTypeDescriptorProviderToken, useClass: TimespecialIsActiveTemporalUnitsTypeDescriptorProviderService, multi: true },
    { provide: ruleTypeDescriptorProviderToken, useClass: UnitStoresUnitRuleTypeDescriptorProviderService, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  public constructor(
    sessionService: SessionService,
    adminLoginService: AdminLoginService,
    translateService: TranslateService,
    private themeService: ThemeService
  ) {
    sessionService.initStore();
    themeService.useTheme('classic');
    const key = AdminLoginService.SESSION_STORAGE_KEY;
    const rawToken: string = sessionStorage.getItem(key);
    const token = JwtTokenUtil.findTokenIfNotExpired(
      rawToken,
      () => sessionStorage.removeItem(key)
    );
    if (token) {
      adminLoginService.defineToken(rawToken);
    }
    const supportedLanguages = ['en', 'es'];
    const browserLang = translateService.getBrowserLang();
    const targetLang = supportedLanguages.some(current => current === browserLang) ? browserLang : 'en';
    translateService.setDefaultLang(targetLang);
  }
}

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
export function findHttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http, './assets/i18n/');
}
