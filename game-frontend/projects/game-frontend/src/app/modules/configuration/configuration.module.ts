import { NgModule, ModuleWithProviders } from '@angular/core';
import { CommonModule } from '@angular/common';

import { CoreModule } from '@owge/core';

import { ConfigurationService } from './services/configuration.service';
import { ConfigurationStore } from './store/configuration.store';

/**
 * This module handles the "Universe configuration"
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.4
 * @export
 * @class ConfigurationModule
 */
@NgModule({
  imports: [
    CommonModule,
    CoreModule,
  ]
})
export class ConfigurationModule {
  public static forRoot(): ModuleWithProviders<ConfigurationModule> {
    return {
      ngModule: ConfigurationModule,
      providers: [ConfigurationStore, ConfigurationService]
    };
  }
}
