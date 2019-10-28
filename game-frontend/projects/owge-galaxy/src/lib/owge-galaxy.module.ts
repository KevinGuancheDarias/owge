import { NgModule } from '@angular/core';
import { PlanetStore } from './stores/planet.store';
import { PlanetService } from './services/planet.service';

@NgModule({
  declarations: [],
  imports: [
  ],
  providers: [
    PlanetStore,
    PlanetService
  ],
  exports: []
})
export class OwgeGalaxyModule { }
