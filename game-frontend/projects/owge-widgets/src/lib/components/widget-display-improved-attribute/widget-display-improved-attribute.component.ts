import { Component, Input, ViewChild, ElementRef, AfterViewInit, OnChanges, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';


/**
 * Display an attribute that may have an improvement papplied
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Component({
  selector: 'owge-widgets-display-improved-attribute',
  templateUrl: './widget-display-improved-attribute.component.html',
  styleUrls: [
    './widget-display-improved-attribute.component.less',
    './widget-display-improved-attribute.component.scss'
  ]
})
export class WidgetDisplayImprovedAttributeComponent implements OnInit, AfterViewInit, OnChanges {

  @Input()
  public name: string;

  @Input()
  public image: string;

  @Input()
  public value: number;

  @Input()
  public maxValue: number;

  @Input()
  public improvementPercentage;

  @Input()
  public staticImage = false;

  @Input()
  public assetsImage = false;

  @Input() public icon: string;

  public numberFormat = '0.0-2';
  public improvementAdd: number;
  public isMouseOver = false;
  public placeHolderTranslationObservable: Observable<string>;

  @ViewChild('textElement', { static: true })
  private _textElement: ElementRef;
  private _maxTextWidth = 120;

  public constructor(private _translateService: TranslateService) { }

  public ngOnInit(): void {
    this.placeHolderTranslationObservable = this._translateService.get('WIDGETS.DISPLAY_IMPROVED_ATTRIBUTE.PLACEHOLDER');
  }

  public ngOnChanges(): void {
    this.improvementAdd = this.value * (this.improvementPercentage / 100);
    this.improvementAdd = isNaN(this.improvementAdd) ? 0 : this.improvementAdd;
  }

  public onMouseEnter(): void {
    this.isMouseOver = true;
  }

  public onMouseLeave(): void {
    this.isMouseOver = false;
  }

  public ngAfterViewInit(): void {
    if (this._textElement) {
      this._textElement.nativeElement.style.visibility = 'hidden';
      const intervalId = setInterval(() => {
        if (this._textElement.nativeElement.offsetWidth > this._maxTextWidth) {
          this._textElement.nativeElement.style.fontSize = `${this._findTextSize(this._textElement.nativeElement) - 1}pt`;
        } else if (typeof this.value === 'number') {
          this._textElement.nativeElement.style.visibility = 'visible';
          clearInterval(intervalId);
        }
      }, 100);
    }
  }

  private _findTextSize(el: HTMLParagraphElement): number {
    return parseInt(el.style.fontSize, 10) || 12;
  }

}
