import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClientModule, HttpClient } from '@angular/common/http';

import {
  CoreModule, OwgeUserModule, SessionService, JwtTokenUtil, LoadingService
} from '@owge/core';
import { OwgeWidgetsModule } from '@owge/widgets';
import { OwgeUniverseModule } from '@owge/universe';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { environment } from '../environments/environment';
import { LoginComponent } from './components/login/login.component';
import { IndexComponent } from './components/index/index.component';
import { AdminLoginService } from './services/admin-login.service';
import { AdminUserStore } from './store/admin-user.store';
import { AdminTimeSpecialService } from './services/admin-time-special.service';
import { CommonCrudComponent } from './components/common-crud/common-crud.component';
import { TimeSpecialCrudComponent } from './components/time-special-crud/time-special-crud.component';
import { CommonCrudWithImageComponent } from './components/common-crud-with-image/common-crud-with-image.component';
import { TranslateModule, TranslateLoader, TranslateService } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { ObjectRequirementsCrudComponent } from './components/object-requirements-crud/object-requirements-crud.component';
import { AdminFactionService } from './services/admin-faction.service';
import { AdminUpgradeService } from './services/admin-upgrade.service';
import { ObjectImprovementsCrudComponent } from './components/object-improvements-crud/object-improvements-crud.component';
import { AdminUnitTypeService } from './services/admin-unit-type.service';
import { UpgradeTypeCrudComponent } from './components/upgrade-type-crud/upgrade-type-crud.component';
import { AdminUpgradeTypeService } from './services/admin-upgrade-type.service';
import { UnitTypeCrudComponent } from './components/unit-type-crud/unit-type-crud.component';
import { FactionCrudComponent } from './components/faction-crud/faction-crud.component';
import { ConfigurationCrudComponent } from './components/configuration-crud/configuration-crud.component';
import { AdminConfigurationService } from './services/admin-configuration.service';
import { GalaxiesCrudComponent } from './components/galaxies-crud/galaxies-crud.component';
import { AdminGalaxyService } from './services/admin-galaxy.service';
import { UpgradeCrudComponent } from './components/upgrade-crud/upgrade-crud.component';
import { ResourceRequirementsCrudComponent } from './components/resource-requirements-crud/resource-requirements-crud.component';
import { UnitCrudComponent } from './components/unit-crud/unit-crud.component';
import { AdminUnitService } from './services/admin-unit.service';
import { SpecialLocationCrudComponent } from './components/special-location-crud/special-location-crud.component';
import { AdminSpecialLocationService } from './services/admin-special-location.service';
import { RequirementsFilterComponent } from './components/requirements-filter/requirements-filter.component';
import { AdminUsersComponent } from './components/admin-users/admin-users.component';
import { AdminUserService } from './services/admin-user.service';

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
    AdminUsersComponent
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
    LoadingService,
    {
      provide: 'APPLICATION_CONTEXT',
      useValue: 'admin'
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  public constructor(
    sessionService: SessionService,
    adminLoginService: AdminLoginService,
    translateService: TranslateService) {
    sessionService.initStore();
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

export function findHttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http, './assets/i18n/');
}
