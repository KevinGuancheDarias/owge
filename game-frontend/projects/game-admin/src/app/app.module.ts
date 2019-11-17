import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClientModule, HttpClient } from '@angular/common/http';

import { CoreModule, OwgeUserModule, SessionService, JwtTokenUtil, LoadingService } from '@owge/core';
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

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    IndexComponent,
    CommonCrudComponent,
    TimeSpecialCrudComponent,
    CommonCrudWithImageComponent,
    ObjectRequirementsCrudComponent,
    ObjectImprovementsCrudComponent
  ],
  imports: [
    BrowserModule,
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
    adminUserStore: AdminUserStore,
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
