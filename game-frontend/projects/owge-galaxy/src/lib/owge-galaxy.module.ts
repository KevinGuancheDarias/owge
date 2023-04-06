import { NgModule } from '@angular/core';
import { PlanetService } from './services/planet.service';
import { PlanetImagePipe } from './pipes/planet-image.pipe';
import { PlanetListAddEditModalComponent } from './components/planet-list-add-edit-modal/planet-list-add-edit-modal.component';
import { CoreModule } from '@owge/core';
import { OwgeWidgetsModule } from '@owge/widgets';
import { PlanetListService } from './services/planet-list.service';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { OwgeSelectedPlanetHttpInterceptor } from './http-interceptors/owge-selected-planet.http-interceptor';

@NgModule({
  declarations: [
    PlanetImagePipe,
    PlanetListAddEditModalComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    CoreModule,
    OwgeWidgetsModule,
    TranslateModule.forChild()
  ],
  providers: [
    PlanetService,
    PlanetListService,
    { provide: HTTP_INTERCEPTORS, useClass: OwgeSelectedPlanetHttpInterceptor, multi: true },
  ],
  exports: [
    PlanetImagePipe,
    PlanetListAddEditModalComponent
  ]
})
export class OwgeGalaxyModule { }
