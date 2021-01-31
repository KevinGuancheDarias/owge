import { Component, Input, OnInit, Output, EventEmitter } from '@angular/core';
import { first } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';

import { AbstractModalContainerComponent } from '@owge/core';

/**
 * Displays a simple modal for confirming an operation <br>
 * <b>NOTICE:</b> In this modal type, closeOnOverlay defaults to false
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
@Component({
  selector: 'owge-widgets-confirmation-dialog',
  templateUrl: './widget-confirmation-dialog.component.html',
  styleUrls: ['./widget-confirmation-dialog.component.less']
})
export class WidgetConfirmationDialogComponent extends AbstractModalContainerComponent implements OnInit {

  @Input() public title: string;
  @Input() public text: string;

  /**
   * Instead of using a <i>text</i> use a i18n name, such as APP.FOO_BAR_BAZ
   *
   * @since 0.9.16
   */
  @Input() public i18nText: string;

  /**
   * Instead of using a <i>title</i> use a i18n title, such as APP.FOO_BAR_BAZ
   *
   * @since 0.9.16
   */
  @Input() public i18nTitle: string;

  /**
   * Represents if the confirmation has been accepted or rejected
   *
   * @since 0.7.0
   */
  @Output() public confirmResult: EventEmitter<boolean> = new EventEmitter();

  public constructor(private _translateService: TranslateService) {
    super();
    this.closeOnOverlayClick = false;
  }

  public async ngOnInit(): Promise<void> {
    this.confirmResult.subscribe(() => this.hide());
    if (!this.i18nTitle && !this.title) {
      this.i18nTitle = 'WIDGETS.CONFIRMATION_DIALOG_DEFAULT_TITLE';
    }
  }
}
