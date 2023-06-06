import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnDestroy } from '@angular/core';
import { LoggerHelper, MEDIA_ROUTES, ThemeService } from '@owge/core';
import { Subscription } from 'rxjs';

@Component({
  selector: 'owge-widgets-display-image',
  templateUrl: './widget-display-image.component.html',
  styleUrls: ['./widget-display-image.component.less'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WidgetDisplayImageComponent implements OnChanges, OnDestroy {
  private static readonly commonTheme = 'common';

  @Input() image: string;
  @Input() title: string;
  @Input() staticImage = false;
  @Input() assetsImage = false;
  @Input() isCommonAssetImage = false;
  @Input() width: number = undefined;
  @Input() height: number = undefined;

  /**
   * @var {string} customClass Can be one or multiple CSS classes, delimited by space
   */
  @Input() customClass: string;

  parsedImage: string;

  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);
  private assetsSubscription: Subscription;

  constructor(private _cdr: ChangeDetectorRef, private themeService: ThemeService) {}

  ngOnChanges(): void {
    if(this.assetsImage) {
      this.resolveAssetImage();
    } else {
      this.unsubscribeAssets();
      this.parsedImage = this.findFullPath(this.image);
      this._cdr.detectChanges();
    }
  }

  ngOnDestroy(): void {
    this.unsubscribeAssets();
  }

  private resolveAssetImage(): void {
    if(this.isCommonAssetImage) {
      this.parsedImage = this.findFullPath(this.image, WidgetDisplayImageComponent.commonTheme);
    } else {
      this.assetsSubscription = this.themeService.currentTheme$.subscribe(theme => {
        this.parsedImage = this.findFullPath(this.image, theme);
      });
    }
  }

  private unsubscribeAssets(): void {
    if(this.assetsSubscription) {
      this.assetsSubscription.unsubscribe();
      delete this.assetsSubscription;
    }
  }

  private findFullPath(image: string, theme?: string) {
    if (this.staticImage) {
      return MEDIA_ROUTES.STATIC_IMAGES_ROOT + image;
    } else if (this.assetsImage) {
      return `/theme/assets/${theme}/img/${image}`;
    } else if (this.isAbsoluteUrl(image) || this.isDynamicAbsolutePath(image)) {
      return image;
    } else {
      this._log.todo([
        'This else clause' +
        'should not be executed in the future when all backend entities are migrated to the same image pattern as TimeSpecial use'
      ]);
      return MEDIA_ROUTES.IMAGES_ROOT + image;
    }
  }

  private isAbsoluteUrl(image: string): boolean {
    return image.startsWith('https://') || image.startsWith('http://');
  }

  private isDynamicAbsolutePath(image: string): boolean {
    return image.startsWith('/dynamic');
  }

}
