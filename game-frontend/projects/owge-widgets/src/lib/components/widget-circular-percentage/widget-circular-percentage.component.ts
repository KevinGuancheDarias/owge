import { Component, AfterViewInit, Input, ViewChild, ElementRef, OnChanges } from '@angular/core';


/**
 *  Paints a circular style percentage
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'owge-widgets-circular-percentage',
  templateUrl: './widget-circular-percentage.component.html',
  styleUrls: ['./widget-circular-percentage.component.scss']
})
export class WidgetCircularPercentageComponent implements OnChanges, AfterViewInit {
  @Input() public percentage: number;
  @Input() public size = 150;

  @ViewChild('progress') private _progress: ElementRef<HTMLDivElement>;
  @ViewChild('left') private _left: ElementRef<HTMLSpanElement>;
  @ViewChild('right') private _right: ElementRef<HTMLSpanElement>;

  constructor() { }

  public ngOnChanges(): void {
    this._handlePaint();
  }

  public ngAfterViewInit(): void {
    this._handlePaint();
  }

  private _handlePaint(): void {
    if (this._progress?.nativeElement) {
      this._progress.nativeElement.style.width = `${this.size}px`;
      this._progress.nativeElement.style.height = `${this.size}px`;
      if (this.percentage > 0) {
        if (this.percentage <= 50) {
          this._right.nativeElement.style.transform = 'rotate(' + this._percentageToDegrees(this.percentage) + 'deg)';
        } else {
          this._right.nativeElement.style.transform = 'rotate(180deg)';
          this._left.nativeElement.style.transform = 'rotate(' + this._percentageToDegrees(this.percentage - 50) + 'deg)';
        }
      }
    }
  }

  private _percentageToDegrees(percentage) {
    return percentage / 100 * 360;
  }

}
