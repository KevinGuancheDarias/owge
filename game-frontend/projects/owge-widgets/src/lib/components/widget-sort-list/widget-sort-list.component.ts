import { Component, EventEmitter, Input, Output } from '@angular/core';

export type SortDirection = 'asc' | 'desc';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.19
 * @export
 */
@Component({
  selector: 'owge-widgets-sort-list',
  templateUrl: './widget-sort-list.component.html',
  styleUrls: ['./widget-sort-list.component.scss']
})
export class WidgetSortListComponent {

  @Input() public isActive = false;
  @Input() public list: any[];
  @Input() public property: string;
  @Input() public direction: SortDirection = 'desc';
  @Output() public sorted: EventEmitter<any[]> = new EventEmitter;

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.19
   */
  public clickSort(): void {
    const directionValue: number = this.direction === 'asc' ? 1 : -1;
    this.direction = this.direction === 'asc' ? 'desc' : 'asc';
    this.sorted.emit(this.list.sort((a, b) => {
      const firstValue: string | unknown = a[this.property];
      const secondValue: string | unknown = b[this.property];
      if (typeof firstValue === 'string' && typeof secondValue === 'string') {
        return firstValue.toLocaleLowerCase() >= secondValue.toLocaleLowerCase() ? directionValue * 1 : directionValue * -1;
      } else {
        return firstValue >= secondValue ? directionValue * 1 : directionValue * -1;
      }
    }));
  }

}
