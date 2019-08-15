import { Component, Input, OnInit, Output, EventEmitter } from '@angular/core';
import {first} from 'rxjs/operators';
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
    if (!this.title) {
      this.title = await this._translateService.get('WIDGETS.CONFIRMATION_DIALOG_DEFAULT_TITLE').pipe(first()).toPromise();
    }
  }
}
