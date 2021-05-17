import { AfterViewInit, Component, ElementRef, Input, ViewChild } from '@angular/core';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
@Component({
  selector: 'owge-widgets-display-single-resource',
  templateUrl: './widget-display-single-resource.component.html',
  styleUrls: [ './widget-display-single-resource.component.scss']
})
export class WidgetDisplaySingleResourceComponent implements AfterViewInit {

  @Input()
  public resourceName: string;

  @Input()
  public resourceImage: string;

  @Input()
  public resourceValue: number;

  @Input()
  public resourceMaxValue: number;

  /**
   * Allows to specify if the number is formatted <br>
   * In previous versions this was always formatted
   *
   * @since 0.9.17
   */
  @Input() public doFormat = true;

  /**
   * Allow to change the image class <br>
   * In previous versions this class was always resourceImage
   *
   * @since 0.9.17
   */
  @Input() public customImageClass = 'resourceImage';

  @Input()
  public staticImage = false;

  @Input()
  public assetsImage = false;

  @Input()
  public usePercentage = false;

  @Input()
  public limitedMax = false;

  @ViewChild('textElement', { static: true })
  private _textElement: ElementRef;

  private _maxTextWidth = 120;

  public ngAfterViewInit(): void {
    this._textElement.nativeElement.style.visibility = 'hidden';
    const intervalId = setInterval(() => {
      if (this._textElement.nativeElement.offsetWidth > this._maxTextWidth) {
        this._textElement.nativeElement.style.fontSize = `${this._findTextSize(this._textElement.nativeElement) - 1}pt`;
      } else {
        this._textElement.nativeElement.style.visibility = 'visible';
        clearInterval(intervalId);
      }
    }, 100);
  }

  private _findTextSize(el: HTMLParagraphElement): number {
    return parseInt(el.style.fontSize, 10) || 12;
  }
}
