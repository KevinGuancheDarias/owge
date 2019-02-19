import { NgModule, ModuleWithProviders } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UniverseStorage } from './storages/universe.storage';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @class UniverseModule
 */
@NgModule({
  imports: [
    CommonModule
  ],
  declarations: []
})
export class UniverseModule {
  public static forRoot(): ModuleWithProviders {
    return {
      ngModule: UniverseModule,
      providers: [UniverseStorage]
    };
  }
}
