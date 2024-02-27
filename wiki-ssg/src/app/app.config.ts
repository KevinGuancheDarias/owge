import {
  APP_INITIALIZER,
  ApplicationConfig,
  importProvidersFrom,
  InjectionToken,
  PLATFORM_ID,
  Provider
} from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import {HttpClient, provideHttpClient, withFetch} from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import {languageInitializer} from './initializer/language.initializer';
import {TranslateLoader, TranslateModule, TranslateService} from '@ngx-translate/core';
import {EnvironmentVariableUtilService} from './services/environment-variable-util.service';
import {translateLoaderFactory} from './initializer/translate-loader-factory';
import {FactionService} from './services/faction.service';
import {servicesInitializer} from './initializer/services.initializer';
import {RequirementsService} from './services/requirements.service';
import {RequirementInformationService} from './services/requirement-information.service';

export const REQUIRING_INIT = new InjectionToken('requiringInit services');

const serviceProviders: Provider[] = [
  { provide: REQUIRING_INIT, useExisting: FactionService, multi: true},
  { provide: REQUIRING_INIT, useExisting: RequirementsService, multi: true},
  { provide: REQUIRING_INIT, useExisting: RequirementInformationService, multi: true},
]

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideClientHydration(),
    provideHttpClient(withFetch()), provideAnimationsAsync(),
    importProvidersFrom(TranslateModule.forRoot({
      defaultLanguage: 'en',
      loader: {
        provide: TranslateLoader,
        useFactory: translateLoaderFactory,
        deps: [HttpClient]
      }
    })),
    serviceProviders,
    {
      provide: APP_INITIALIZER,
      useFactory: languageInitializer,
      multi: true,
      deps: [PLATFORM_ID, TranslateService, EnvironmentVariableUtilService]
    },
    {
      provide: APP_INITIALIZER,
      useFactory: servicesInitializer,
      multi: true,
      deps: [REQUIRING_INIT]
    }
  ]
};
