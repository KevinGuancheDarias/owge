import { Component, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'owge-widgets-time-selection',
  templateUrl: './widget-time-selection.component.html',
  styleUrls: ['./widget-time-selection.component.scss']
})
export class WidgetTimeSelectionComponent {
  @Output() timeChanged: EventEmitter<number> = new EventEmitter;

  days = 0;
  hours = 0;
  minutes = 0;
  seconds = 0;

  onValuesChanged(): void {
    this.timeChanged.emit(this.calculateSeconds());
  }

  private calculateSeconds(): number {
    return (this.days * 86400) + (this.hours * 3600) + (this.minutes * 60) + this.seconds;
  }
}
