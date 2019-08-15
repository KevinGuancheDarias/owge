import { NgModule, ModuleWithProviders } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UniverseStorage } from './storages/universe.storage';
import { UniverseInitializer } from './initializers/universe.initializer';
import { UniverseService } from './services/universe.service';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
@NgModule({
  imports: [
    CommonModule
  ],
  declarations: []
})
export class OwgeUniverseModule {
  public static forRoot(): ModuleWithProviders {
    return {
      ngModule: OwgeUniverseModule,
      providers: [UniverseStorage, UniverseService, UniverseInitializer]
    };
  }
}
