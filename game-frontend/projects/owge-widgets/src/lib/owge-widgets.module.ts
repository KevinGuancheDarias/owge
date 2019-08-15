import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { CoreModule } from '@owge/core';

import { WidgetConfirmationDialogComponent } from './components/widget-confirmation-dialog/widget-confirmation-dialog.component';

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
    TranslateModule.forChild()
  ],
  declarations: [WidgetConfirmationDialogComponent],
  exports: [WidgetConfirmationDialogComponent]
})
export class OwgeWidgetsModule {
}
