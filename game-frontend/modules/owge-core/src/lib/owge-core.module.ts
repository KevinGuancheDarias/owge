import { CommonModule } from '@angular/common';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { ModuleWithProviders, NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { LoadingComponent } from './components/loading/loading.component';
import { ModalComponent } from './components/modal/modal.component';
import { PageNotFoundComponent } from './components/page-not-found/page-not-found.component';
import { RouterRootComponent } from './components/router-root/router-root.component';
import { OwgeCoreIfDesktopDirective } from './directives/if-desktop.directive';
import { OwgeCoreIfThemeDirective } from './directives/if-theme.directive';
import { OwgeContentDirective } from './directives/owge-content.directive';
import { LanguageHttpInterceptor } from './http-interceptors/language.http-interceptor';
import { DynamicImagePipe } from './pipes/dynamic-image.pipe';
import { FormatDateRepresentation } from './pipes/format-date-representation';
import { FormatNumberPipe } from './pipes/format-number.pipe';
import { HideDuplicatedNamePipe } from './pipes/hide-duplicated-name.pipe';
import { Config } from './pojos/config.pojo';
import { OwgeCoreConfig } from './pojos/owge-core-config';
import { CoreHttpService } from './services/core-http.service';
import { LocalConfigurationService } from './services/local-configuration.service';
import { ScreenDimensionsService } from './services/screen-dimensions.service';
import { ThemeService } from './services/theme.service';
import { ToastrService } from './services/toastr.service';
import { WarningWebsocketApplicationHandlerService } from './services/warning-websocket-application-handler.service';
import {TypeofPipe} from './pipes/typeof.pipe';

/**
 * Has the shared features between modules, such as loading image
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild([]),
    TranslateModule.forChild()
  ],
  declarations: [
    LoadingComponent,
    RouterRootComponent,
    ModalComponent,
    PageNotFoundComponent,
    HideDuplicatedNamePipe,
    OwgeContentDirective,
    OwgeCoreIfDesktopDirective,
    DynamicImagePipe,
    FormatNumberPipe,
    FormatDateRepresentation,
    OwgeCoreIfThemeDirective,
    TypeofPipe
  ],
  providers: [
    ScreenDimensionsService,
    LocalConfigurationService
  ],
  exports: [
    LoadingComponent,
    RouterRootComponent,
    ModalComponent,
    PageNotFoundComponent,
    HideDuplicatedNamePipe,
    OwgeContentDirective,
    OwgeCoreIfDesktopDirective,
    DynamicImagePipe,
    FormatNumberPipe,
    FormatDateRepresentation,
    OwgeCoreIfThemeDirective,
    TypeofPipe
  ],
})
export class CoreModule {

  /**
   * Creates an instance of CoreModule.
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param accountConfig
   */
  public constructor(accountConfig: OwgeCoreConfig) {
    Config.accountServerUrl = accountConfig.url;
    Config.accountLoginendpoint = accountConfig.loginEndpoint;
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param accountConfig
   * @returns
   */
  public static forRoot(accountConfig: OwgeCoreConfig): ModuleWithProviders<CoreModule> {
    return {
      ngModule: CoreModule,
      providers: [
        CoreHttpService,
        { provide: OwgeCoreConfig, useValue: accountConfig },
        { provide: HTTP_INTERCEPTORS, useClass: LanguageHttpInterceptor, multi: true },
        ScreenDimensionsService,
        LocalConfigurationService,
        ToastrService,
        WarningWebsocketApplicationHandlerService,
        ThemeService
      ]
    };
  }

}
