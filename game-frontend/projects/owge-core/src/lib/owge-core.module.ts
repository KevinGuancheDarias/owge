import { NgModule, ModuleWithProviders } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoadingComponent } from './components/loading/loading.component';
import { RouterRootComponent } from './components/router-root/router-root.component';
import { RouterModule } from '@angular/router';
import { CoreHttpService } from './services/core-http.service';
import { ModalComponent } from './components/modal/modal.component';
import { TranslateModule } from '@ngx-translate/core';
import { OwgeCoreConfig } from './pojos/owge-core-config';
import { Config } from './pojos/config.pojo';
import { PageNotFoundComponent } from './components/page-not-found/page-not-found.component';
import { ScreenDimensionsService } from './services/screen-dimensions.service';
import { HideDuplicatedNamePipe } from './pipes/hide-duplicated-name.pipe';
import { OwgeContentDirective } from './directives/owge-content.directive';
import { OwgeCoreIfDesktopDirective } from './directives/if-desktop.directive';
import { LocalConfigurationService } from './services/local-configuration.service';
import { DynamicImagePipe } from './pipes/dynamic-image.pipe';

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
    DynamicImagePipe
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
    DynamicImagePipe
  ],
})
export class CoreModule {

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param accountConfig
   * @returns
   */
  public static forRoot(accountConfig: OwgeCoreConfig): ModuleWithProviders {
    return {
      ngModule: CoreModule,
      providers: [
        CoreHttpService,
        { provide: OwgeCoreConfig, useValue: accountConfig },
        ScreenDimensionsService,
        LocalConfigurationService
      ]
    };
  }

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
}
