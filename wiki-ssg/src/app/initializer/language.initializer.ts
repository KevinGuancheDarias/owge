import {TranslateService} from '@ngx-translate/core';
import {isPlatformBrowser} from '@angular/common';
import {EnvironmentVariableUtilService} from '../services/environment-variable-util.service';
import {firstValueFrom} from 'rxjs';

const supportedLanguages = ['es','en'];
function validLanguageOrDefault(lang: string) {
  return supportedLanguages.find(current => current === lang) ?? 'en';
}

export function languageInitializer(
  platformId: Object, translateService: TranslateService, environmentVariableService: EnvironmentVariableUtilService
): () => Promise<void> {
  return async () => {
    if (isPlatformBrowser(platformId)) {
      await firstValueFrom(translateService.use(validLanguageOrDefault(translateService.getBrowserLang() ?? 'en')));
    } else {
      await firstValueFrom(translateService.use(environmentVariableService.getOrFail('OWGE_SSR_LANG')));
    }
  }
}
