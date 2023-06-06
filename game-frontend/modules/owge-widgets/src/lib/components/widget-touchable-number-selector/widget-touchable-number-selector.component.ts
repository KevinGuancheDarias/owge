import { Component, EventEmitter, Input, OnChanges, OnInit, Output } from '@angular/core';
import { ProgrammingError } from '@owge/core';

type ValidTarget = 'PLUS' | 'MINUS';

@Component({
  selector: 'owge-widgets-touchable-number-selector',
  templateUrl: './widget-touchable-number-selector.component.html',
  styleUrls: ['./widget-touchable-number-selector.component.scss']
})
export class WidgetTouchableNumberSelectorComponent implements OnInit, OnChanges {

  private static readonly defaultLatency = 400;
  private static readonly minLatency = 100;

  @Input() title: string;
  @Input() minValue = 0;
  @Input() maxValue: number;
  @Input() currentValue: number;
  @Output() currentValueChange: EventEmitter<number> = new EventEmitter;
  @Output() maxValuePassed: EventEmitter<void> = new EventEmitter;
  @Output() minValuePassed: EventEmitter<void> = new EventEmitter;

  defaultValue = 0;

  private differenceBetweenMinAndMaxValue: number;
  private mouseDownIntervalId: number;
  private currentLatency = WidgetTouchableNumberSelectorComponent.defaultLatency;

  constructor() { }

  ngOnInit(): void {
    if (!this.currentValue) {
      this.currentValue = this.defaultValue;
    }
  }

  ngOnChanges(): void {
    this.differenceBetweenMinAndMaxValue = this.maxValue - this.minValue;
    this.handleValueRangePassed();
  }

  clickPlus(): void {
    this.currentValue++;
    this.handleValueRangePassed();
  }

  clickMinus(): void {
    this.currentValue--;
    this.handleValueRangePassed();
  }

  onMouseDown(target: ValidTarget): void {
    this.mouseDownIntervalId = window.setInterval(() => this.whileMousePress(target), this.currentLatency);
  }

  onMouseUp(): void {
    this.currentLatency = WidgetTouchableNumberSelectorComponent.defaultLatency;
    if (this.mouseDownIntervalId) {
      window.clearInterval(this.mouseDownIntervalId);
      delete this.mouseDownIntervalId;
    }
  }

  whileMousePress(target: ValidTarget): void {
    if (this.currentLatency > WidgetTouchableNumberSelectorComponent.minLatency) {
      this.currentLatency -= 20;
      window.clearInterval(this.mouseDownIntervalId);
      this.onMouseDown(target);
    }

    if (target === 'PLUS') {
      this.clickPlus();
    } else if (target === 'MINUS') {
      this.clickMinus();
    } else {
      throw new ProgrammingError(`Invalid target ${target}`);
    }
  }

  private handleValueRangePassed() {
    if (this.currentValue < this.minValue) {
      const passedCount: number = this.minValue - this.currentValue;
      this.currentValue = this.maxValue + passedCount - 1;
      this.minValuePassed.emit();
    } else if (this.currentValue > this.maxValue) {
      const passedCount: number = this.currentValue - this.maxValue;
      this.currentValue = passedCount - this.minValue;
      this.maxValuePassed.emit();
    }
    this.currentValueChange.emit(this.currentValue);
  }
}
