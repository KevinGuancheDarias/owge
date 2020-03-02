import { Component, Input, OnChanges, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';

import { MEDIA_ROUTES, LoggerHelper } from '@owge/core';

@Component({
  selector: 'owge-widgets-display-image',
  templateUrl: './widget-display-image.component.html',
  styleUrls: ['./widget-display-image.component.less'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WidgetDisplayImageComponent implements OnChanges {
  /**
     * Returns the full path to the image
     *
     * @author Kevin Guanche Darias
     */
  public parsedImage: string;

  @Input()
  public image: string;

  @Input()
  public title: string;

  @Input()
  public staticImage = false;

  @Input()
  public assetsImage = false;

  /**
   * @var {string} Can be one or multiple CSS classes, delimited by space
   */
  @Input()
  public customClass: string;

  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);

  public constructor(private _cdr: ChangeDetectorRef) { }

  public ngOnChanges(): void {
    this.parsedImage = this._findFullPath(this.image);
    this._cdr.detectChanges();
  }

  private _findFullPath(image: string) {
    if (this.staticImage) {
      return MEDIA_ROUTES.STATIC_IMAGES_ROOT + image;
    } else if (this.assetsImage) {
      return `/assets/img/${image}`;
    } else if (this._isAbsoluteUrl(image) || this._isDynamicAbsolutePath(image)) {
      return image;
    } else {
      this._log.todo([
        'This else clause' +
        'should not be executed in the future when all backend entities are migrated to the same image pattern as TimeSpecial use'
      ]);
      return MEDIA_ROUTES.IMAGES_ROOT + image;
    }
  }

  private _isAbsoluteUrl(image: string): boolean {
    return image.startsWith('https://') || image.startsWith('http://');
  }

  private _isDynamicAbsolutePath(image: string): boolean {
    return image.startsWith('/dynamic');
  }

}
