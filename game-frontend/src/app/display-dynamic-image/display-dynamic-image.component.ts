import { MEDIA_ROUTES } from '../config/config.pojo';
import { Component, Input } from '@angular/core';

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
  public get parsedImage(): string{
    return this._findFullPath(this.image);
  }

  @Input()
  public image: string;

  @Input()
  public title: string;

  @Input()
  public staticImage: boolean = false;

  /**
   * @var {string} Can be one or multiple CSS classes, delimited by space
   */
  @Input()
  public customClass: string;

  private _findFullPath(image: string) {
    if (this.staticImage) {
      return MEDIA_ROUTES.STATIC_IMAGES_ROOT + image;
    } else {
      return MEDIA_ROUTES.IMAGES_ROOT + image;
    }
  }
}
