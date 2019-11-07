import { Component, Input } from '@angular/core';

import { MEDIA_ROUTES, LoggerHelper } from '@owge/core';


/**
 *
 * @deprecated As of 0.8.0 use OwgeWidgetsModule/WidgetDisplayDynamicImage
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
@Component({
  selector: 'app-display-dynamic-image',
  templateUrl: './display-dynamic-image.component.html',
  styleUrls: ['./display-dynamic-image.component.less']
})
export class DisplayDynamicImageComponent {

  /**
   * Returns the full path to the image
   *
   * @author Kevin Guanche Darias
   */
  public get parsedImage(): string {
    return this._findFullPath(this.image);
  }

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

  public constructor() {
    this._log.warnDeprecated(this.constructor.name, '0.8.0', 'OwgeWidgetsModule/WidgetDisplayDynamicImage');
  }

  private _findFullPath(image: string) {
    if (this.staticImage) {
      return MEDIA_ROUTES.STATIC_IMAGES_ROOT + image;
    } else if (this.assetsImage) {
      return `/assets/img/${image}`;
    } else {
      return MEDIA_ROUTES.IMAGES_ROOT + image;
    }
  }
}
