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

  public parsedImage: string;

  private _log: LoggerHelper = new LoggerHelper(this.constructor.name);
  private currentTheme: string;
  private assetsSubscription: Subscription;

  public constructor(private _cdr: ChangeDetectorRef, private themeService: ThemeService) {}

  public ngOnChanges(): void {
    if(this.assetsImage) {
      this.assetsSubscription = this.themeService.currentTheme$.subscribe(theme => {
          this.currentTheme = theme;
          this.parsedImage = this._findFullPath(this.image);
        });
    } else {
      this.unsubscribeAssets();
      this.parsedImage = this._findFullPath(this.image);
      this._cdr.detectChanges();
    }
  }

  public ngOnDestroy(): void {
    this.unsubscribeAssets();
  }

  private unsubscribeAssets(): void {
    if(this.assetsSubscription) {
      this.assetsSubscription.unsubscribe();
      delete this.assetsSubscription;
    }
  }

  private _findFullPath(image: string) {
    if (this.staticImage) {
      return MEDIA_ROUTES.STATIC_IMAGES_ROOT + image;
    } else if (this.assetsImage) {
      return `/theme/assets/${this.currentTheme}/img/${image}`;
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
