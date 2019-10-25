import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { CoreModule } from '@owge/core';

import { WidgetConfirmationDialogComponent } from './components/widget-confirmation-dialog/widget-confirmation-dialog.component';
import { WidgetSideBarComponent } from './components/widget-sidebar/widget-sidebar.component';
import { DisplayService } from './services/display.service';
import { RemovableImageComponent } from './components/removable-image/removable-image.component';

/**
 * Has UI widgets for OWGE
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
@NgModule({
  imports: [
    CommonModule,
    CoreModule,
    RouterModule,
    TranslateModule.forChild()
  ],
  declarations: [
    WidgetConfirmationDialogComponent,
    WidgetSideBarComponent,
    RemovableImageComponent
  ],
  providers: [
    DisplayService
  ],
  exports: [WidgetConfirmationDialogComponent, WidgetSideBarComponent, RemovableImageComponent]
})
export class OwgeWidgetsModule {
}
