import { Component, OnInit, Output, EventEmitter, ViewChild, Input } from '@angular/core';
import { ModalComponent } from '@owge/core';
import { WidgetConfirmationDialogComponent } from '../widget-confirmation-dialog/widget-confirmation-dialog.component';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'owge-widgets-choose-item-modal',
  templateUrl: './widget-choose-item-modal.component.html',
  styleUrls: ['./widget-choose-item-modal.component.scss']
})
export class WidgetChooseItemModalComponent {

  @Input() public itemName: string;
  @Input() public i18nModalTitle: string;
  @Input() public i18nNullValue: string;
  @Input() public isDisabled: boolean;
  @Output() public save: EventEmitter<void> = new EventEmitter;
  @Output() public cancel: EventEmitter<void> = new EventEmitter;
  @Output() public delete: EventEmitter<void> = new EventEmitter;
  @ViewChild(ModalComponent) public modal: ModalComponent;
  @ViewChild(WidgetConfirmationDialogComponent) public confirmationDialog: WidgetConfirmationDialogComponent;

  constructor() { }

  /**
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   */
  public clickSave(): void {
    this.save.emit();
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param isConfirmed
   */
  public clickDelete(isConfirmed: boolean): void {
    if (isConfirmed) {
      this.delete.emit();
    }
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   */
  public clickCancel(): void {
    this.hide();
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   */
  public hide(): void {
    this.modal.hide();
  }
}
