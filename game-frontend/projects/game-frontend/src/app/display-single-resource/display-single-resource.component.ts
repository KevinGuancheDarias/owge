import { Component, Input, ViewChild, ElementRef, AfterViewInit } from '@angular/core';

@Component({
  selector: 'app-display-single-resource',
  templateUrl: './display-single-resource.component.html',
  styleUrls: [
    './display-single-resource.component.less',
    './display-single-resource.component.scss'
  ]
})
export class DisplaySingleResourceComponent implements AfterViewInit {

  @Input()
  public resourceName: string;

  @Input()
  public resourceImage: string;

  @Input()
  public resourceValue: number;

  @Input()
  public resourceMaxValue: number;

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
      } else if (typeof this.resourceValue === 'number') {
        this._textElement.nativeElement.style.visibility = 'visible';
        clearInterval(intervalId);
      }
    }, 100);
  }

  private _findTextSize(el: HTMLParagraphElement): number {
    return parseInt(el.style.fontSize, 10) || 12;
  }
}
