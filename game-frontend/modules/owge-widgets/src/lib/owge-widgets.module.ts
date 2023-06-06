import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { CoreModule } from '@owge/core';
import { OwgeCardListComponent } from './components/owge-card-list/owge-card-list.component';
import { RemovableImageComponent } from './components/removable-image/removable-image.component';
import { WidgetChooseItemModalComponent } from './components/widget-choose-item-modal/widget-choose-item-modal.component';
import { WidgetCircularPercentageComponent } from './components/widget-circular-percentage/widget-circular-percentage.component';
import { WidgetCollapsableItemComponent } from './components/widget-collapsable-item/widget-collapsable-item.component';
import { WidgetConfirmationDialogComponent } from './components/widget-confirmation-dialog/widget-confirmation-dialog.component';
import { WidgetCountdownComponent } from './components/widget-countdown/widget-countdown.component';
import { WidgetDisplayImageComponent } from './components/widget-display-dynamic-image/widget-display-image.component';
import {
  WidgetDisplayImprovedAttributeComponent
} from './components/widget-display-improved-attribute/widget-display-improved-attribute.component';
import { WidgetDisplayImprovementsComponent } from './components/widget-display-improvements/widget-display-improvements.component';
import { WidgetDisplayListItemComponent } from './components/widget-display-list-item/widget-display-list-item.component';
import { WidgetDisplaySimpleItemComponent } from './components/widget-display-simple-item/widget-display-simple-item.component';
import { WidgetDisplaySingleResourceComponent } from './components/widget-display-single-resource/widget-display-single-resource.component';
import {
  WidgetDisplayUnitImprovementsComponent
} from './components/widget-display-unit-improvements/widget-display-unit-improvements.component';
import { WidgetFiltrableSelectComponent } from './components/widget-filtrable-select/widget-filtrable-select.component';
import { WidgetIdNameDropdownComponent } from './components/widget-id-name-dropdown/widget-id-name-dropdown.component';
import { WidgetSideBarComponent } from './components/widget-sidebar/widget-sidebar.component';
import { WidgetSortListComponent } from './components/widget-sort-list/widget-sort-list.component';
import { WidgetSpanWithPlaceholderComponent } from './components/widget-span-with-placeholder/widget-span-with-placeholder.component';
import { WidgetTimeSelectionComponent } from './components/widget-time-selection/widget-time-selection.component';
import {
  WidgetTouchableNumberSelectorComponent
} from './components/widget-touchable-number-selector/widget-touchable-number-selector.component';
import { WidgetWarnMessageComponent } from './components/widget-warn-message/widget-warn-message.component';
import { UiIconPipe } from './pipes/ui-icon.pipe';
import { DisplayService } from './services/display.service';



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
    WidgetSortListComponent,
    WidgetWarnMessageComponent,
    WidgetTimeSelectionComponent,
    WidgetTouchableNumberSelectorComponent,
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
    WidgetSortListComponent,
    WidgetWarnMessageComponent,
    WidgetTimeSelectionComponent,
    WidgetTouchableNumberSelectorComponent
  ]
})
export class OwgeWidgetsModule {
}
