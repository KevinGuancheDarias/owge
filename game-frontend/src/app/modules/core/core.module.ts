import { NgModule, ModuleWithProviders, Provider, APP_INITIALIZER } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoadingComponent } from '../../loading/loading.component';
import { RouterRootComponent } from './components/router-root/router-root.component';
import { RouterModule } from '@angular/router';
import { CoreGameService } from './services/core-game.service';
import { CoreHttpService } from './services/core-http.service';
import { UniverseModule } from '../universe/universe.module';
import { ModalComponent } from '../../components/modal/modal.component';
import { TranslateModule } from '@ngx-translate/core';

/**
 * Has the shared features between modules, such as loading image
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @class CoreModule
 */
@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild([]),
    UniverseModule,
    TranslateModule.forChild()
  ],
  declarations: [LoadingComponent, RouterRootComponent, ModalComponent],
  exports: [LoadingComponent, RouterRootComponent, ModalComponent]
})
export class CoreModule {

  public static forRoot(): ModuleWithProviders {
    return {
      ngModule: CoreModule,
      providers: [CoreGameService, CoreHttpService]
    };
  }
}
