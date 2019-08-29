import { NgModule, ModuleWithProviders } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoadingComponent } from './components/loading/loading.component';
import { RouterRootComponent } from './components/router-root/router-root.component';
import { RouterModule } from '@angular/router';
import { CoreHttpService } from './services/core-http.service';
import { ModalComponent } from './components/modal/modal.component';
import { TranslateModule } from '@ngx-translate/core';
import { AccountConfig } from './pojos/account-config.pojo';
import { Config } from './pojos/config.pojo';
import { PageNotFoundComponent } from './components/page-not-found/page-not-found.component';

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
  declarations: [LoadingComponent, RouterRootComponent, ModalComponent, PageNotFoundComponent],
  exports: [LoadingComponent, RouterRootComponent, ModalComponent, PageNotFoundComponent]
})
export class CoreModule {

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param accountConfig
   * @returns
   */
  public static forRoot(accountConfig: AccountConfig): ModuleWithProviders {
    return {
      ngModule: CoreModule,
      providers: [
        CoreHttpService,
        { provide: AccountConfig, useValue: accountConfig}
      ]
    };
  }

  /**
   * Creates an instance of CoreModule.
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param accountConfig
   */
  public constructor (accountConfig: AccountConfig) {
    Config.accountServerUrl = accountConfig.url;
    Config.accountLoginendpoint = accountConfig.loginEndpoint;
  }
}
