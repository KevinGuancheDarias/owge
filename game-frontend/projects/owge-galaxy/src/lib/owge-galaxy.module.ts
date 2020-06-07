import { NgModule } from '@angular/core';
import { PlanetService } from './services/planet.service';
import { PlanetImagePipe } from './pipes/planet-image.pipe';

@NgModule({
  declarations: [
    PlanetImagePipe
  ],
  imports: [
  ],
  providers: [
    PlanetService
  ],
  exports: [
    PlanetImagePipe
  ]
})
export class OwgeGalaxyModule { }
