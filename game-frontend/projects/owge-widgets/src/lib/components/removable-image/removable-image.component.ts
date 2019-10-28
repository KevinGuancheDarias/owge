import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';

import { LoggerHelper } from '@owge/core';


/**
 * Used to mark an image as deletable
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Component({
  selector: 'owge-widgets-removable-image',
  templateUrl: './removable-image.component.html',
  styleUrls: ['./removable-image.component.less']
})
export class RemovableImageComponent {
  /**
   * The CSS class to use for the div
   *
   */
  @Input() public cssClass: string;

  /**
   * How many time in <b>seconds</b> to displat the undo button
   *
   */
  @Input() public undoDelay = 5;

  /**
   * The passed image
   */
  @Input() public imageUrl: string;
  @Output() public imageUrlChange: EventEmitter<string> = new EventEmitter();

  /**
   * Fires when the image has been removed, has the old image value
   *
   */
  @Output() public deleted: EventEmitter<string> = new EventEmitter();

  public undoTimer = 0;

  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);
  private _undoInterval: number;
  private _originalImage: string;

  /**
   * Removes the image (usually on click on delete icon xD)
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   */
  public remove(): void {
    this._originalImage = this.imageUrl;
    this._log.debug(`Removed image ${this._originalImage}`);
    this.deleted.emit(this.imageUrl);
    this._changeImage(null);
    this.undoTimer = this.undoDelay;
    this._undoInterval = window.setInterval(() => {
      this.undoTimer--;
      if (!this.undoTimer) {
        this._removeInterval();
      }
    }, 1000);
  }

  public undo(): void {
    if (this._undoInterval) {
      this._changeImage(this._originalImage);
      this._removeInterval();
    }
  }

  private _changeImage(image: string = null): void {
    this.imageUrl = image;
    this.imageUrlChange.emit(this.imageUrl);
  }

  private _removeInterval(): void {
    window.clearInterval(this._undoInterval);
    this._undoInterval = 0;
  }
}
