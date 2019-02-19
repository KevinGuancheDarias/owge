import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { WidgetConfirmationDialogComponent } from './components/widget-confirmation-dialog/widget-confirmation-dialog.component';
import { CoreModule } from '../core/core.module';

/**
 * Has UI widgets for SGT
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @class WidgetsModule
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
export class WidgetsModule {
}
