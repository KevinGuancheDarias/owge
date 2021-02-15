import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { CoreModule } from '@owge/core';

import { WidgetConfirmationDialogComponent } from './components/widget-confirmation-dialog/widget-confirmation-dialog.component';
import { WidgetSideBarComponent } from './components/widget-sidebar/widget-sidebar.component';
import { DisplayService } from './services/display.service';
import { RemovableImageComponent } from './components/removable-image/removable-image.component';
import { OwgeCardListComponent } from './components/owge-card-list/owge-card-list.component';
import {
  WidgetDisplayImprovedAttributeComponent
} from './components/widget-display-improved-attribute/widget-display-improved-attribute.component';
import { WidgetDisplayImageComponent } from './components/widget-display-dynamic-image/widget-display-image.component';
import { WidgetSpanWithPlaceholderComponent } from './components/widget-span-with-placeholder/widget-span-with-placeholder.component';
import { UiIconPipe } from './pipes/ui-icon.pipe';
import { WidgetDisplayListItemComponent } from './components/widget-display-list-item/widget-display-list-item.component';
import { WidgetCountdownComponent } from './components/widget-countdown/widget-countdown.component';
import { WidgetCollapsableItemComponent } from './components/widget-collapsable-item/widget-collapsable-item.component';
import { WidgetDisplaySimpleItemComponent } from './components/widget-display-simple-item/widget-display-simple-item.component';
import { WidgetIdNameDropdownComponent } from './components/widget-id-name-dropdown/widget-id-name-dropdown.component';
import { FormsModule } from '@angular/forms';
import { WidgetFiltrableSelectComponent } from './components/widget-filtrable-select/widget-filtrable-select.component';
import { WidgetCircularPercentageComponent } from './components/widget-circular-percentage/widget-circular-percentage.component';
import { WidgetChooseItemModalComponent } from './components/widget-choose-item-modal/widget-choose-item-modal.component';
import { WidgetDisplayImprovementsComponent } from './components/widget-display-improvements/widget-display-improvements.component';
import { WidgetDisplaySingleResourceComponent } from './components/widget-display-single-resource/widget-display-single-resource.component';
import {
  WidgetDisplayUnitImprovementsComponent
} from './components/widget-display-unit-improvements/widget-display-unit-improvements.component';
import { WidgetSortListComponent } from './components/widget-sort-list/widget-sort-list.component';

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
    TranslateModule.forChild(),
    FormsModule
  ],
  declarations: [
    WidgetConfirmationDialogComponent,
    WidgetSideBarComponent,
    RemovableImageComponent,
    OwgeCardListComponent,
    WidgetDisplayImprovedAttributeComponent,
    WidgetDisplayImageComponent,
    WidgetSpanWithPlaceholderComponent,
    UiIconPipe,
    WidgetDisplayListItemComponent,
    WidgetCountdownComponent,
    WidgetCollapsableItemComponent,
    WidgetDisplaySimpleItemComponent,
    WidgetIdNameDropdownComponent,
    WidgetFiltrableSelectComponent,
    WidgetCircularPercentageComponent,
    WidgetChooseItemModalComponent,
    WidgetDisplayImprovementsComponent,
    WidgetDisplaySingleResourceComponent,
    WidgetDisplayUnitImprovementsComponent,
    WidgetSortListComponent
  ],
  providers: [
    DisplayService
  ],
  exports: [
    WidgetConfirmationDialogComponent,
    WidgetSideBarComponent,
    RemovableImageComponent,
    OwgeCardListComponent,
    WidgetDisplayImprovedAttributeComponent,
    WidgetDisplayImageComponent,
    WidgetSpanWithPlaceholderComponent,
    UiIconPipe,
    WidgetDisplayListItemComponent,
    WidgetCountdownComponent,
    WidgetCollapsableItemComponent,
    WidgetDisplaySimpleItemComponent,
    WidgetIdNameDropdownComponent,
    WidgetFiltrableSelectComponent,
    WidgetCircularPercentageComponent,
    WidgetChooseItemModalComponent,
    WidgetDisplayImprovementsComponent,
    WidgetDisplaySingleResourceComponent,
    WidgetSortListComponent
  ]
})
export class OwgeWidgetsModule {
}
